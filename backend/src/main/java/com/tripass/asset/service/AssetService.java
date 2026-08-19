package com.tripass.asset.service;

import com.tripass.asset.dto.*;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.asset.mapper.AssetMapper;
import com.tripass.asset.service.codef.CodefClient;
import com.tripass.common.exception.CustomException;
import com.tripass.saving.classification.CategoryClassificationResult;
import com.tripass.saving.classification.CategorySource;
import com.tripass.saving.classification.TransactionCategoryClassifier;
import com.tripass.saving.service.MonthlySpendingAnalysisService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Log4j2
@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetMapper assetMapper;
    private final TransactionCategoryClassifier transactionCategoryClassifier;
    private final DuplicateTransactionMatcher duplicateTransactionMatcher;
    private final CodefClient codefClient;
    private final MonthlySpendingAnalysisService monthlySpendingAnalysisService;

    public AssetService(
            AssetMapper assetMapper,
            TransactionCategoryClassifier transactionCategoryClassifier,
            DuplicateTransactionMatcher duplicateTransactionMatcher,
            CodefClient codefClient,
            MonthlySpendingAnalysisService monthlySpendingAnalysisService
    ) {
        this.assetMapper = assetMapper;
        this.transactionCategoryClassifier = transactionCategoryClassifier;
        this.duplicateTransactionMatcher = duplicateTransactionMatcher;
        this.codefClient = codefClient;
        this.monthlySpendingAnalysisService = monthlySpendingAnalysisService;
    }

    @Transactional
    public List<AccountDto> linkBank(Long userId, CodefLinkRequestDto req) {
        try {
            String encryptedPw = codefClient.encodePassword(req.getPassword());
            String accessToken = codefClient.getAccessToken();

            CodefConnectionDto existingConn = assetMapper.findConnectionByUserId(userId);
            String connectedId;
            boolean institutionAlreadyRegistered = false;

            if (existingConn == null) {
                // 신규: connectedId 발급
                Map<String, Object> account = new HashMap<>();
                account.put("countryCode", "KR");
                account.put("businessType", req.getBusinessType());
                account.put("clientType", "P");
                account.put("organization", req.getOrganizationCode());
                account.put("loginType", req.getLoginType());
                account.put("id", req.getLoginId());
                account.put("password", encryptedPw);

                Map<String, Object> createBody = new HashMap<>();
                createBody.put("accountList", List.of(account));

                Map<String, Object> createResult = codefClient.callApi(accessToken, "/v1/account/create", createBody);
                Map<String, Object> createResultCode = (Map<String, Object>) createResult.get("result");
                if (createResultCode == null || !"CF-00000".equals(createResultCode.get("code"))) {
                    String msg = createResultCode != null ? (String) createResultCode.get("message") : "알 수 없는 오류";
                    throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "계좌 등록 실패: " + msg);
                }
                Map<String, Object> data = (Map<String, Object>) createResult.get("data");
                connectedId = (String) data.get("connectedId");

                CodefConnectionDto newConn = new CodefConnectionDto();
                newConn.setUserId(userId);
                newConn.setConnectedId(connectedId);
                assetMapper.insertCodefConnection(newConn);
                existingConn = assetMapper.findConnectionByUserId(userId);
            } else {
                // 기존 connectedId에 기관 추가
                connectedId = existingConn.getConnectedId();
                CodefConnectedInstitutionDto registered = assetMapper.findConnectedInstitution(
                        existingConn.getId(), req.getOrganizationCode(), req.getBusinessType());
                institutionAlreadyRegistered = registered != null;

                if (!institutionAlreadyRegistered) {
                    Map<String, Object> addAccount = new HashMap<>();
                    addAccount.put("countryCode", "KR");
                    addAccount.put("businessType", req.getBusinessType());
                    addAccount.put("clientType", "P");
                    addAccount.put("organization", req.getOrganizationCode());
                    addAccount.put("loginType", req.getLoginType());
                    addAccount.put("id", req.getLoginId());
                    addAccount.put("password", encryptedPw);

                    Map<String, Object> addBody = new HashMap<>();
                    addBody.put("connectedId", connectedId);
                    addBody.put("accountList", List.of(addAccount));

                    Map<String, Object> addResult = codefClient.callApi(accessToken, "/v1/account/add", addBody);
                    Map<String, Object> addResultCode = (Map<String, Object>) addResult.get("result");
                    if (addResultCode == null || !"CF-00000".equals(addResultCode.get("code"))) {
                        // CODEF에는 등록됐지만 로컬 기관 기록이 누락된 상태인지 목록 조회로 확인한다.
                        Map<String, Object> listCheckBody = new HashMap<>();
                        listCheckBody.put("connectedId", connectedId);
                        listCheckBody.put("organization", req.getOrganizationCode());
                        listCheckBody.put("startDate", "19000101");
                        listCheckBody.put("endDate", "99991231");
                        listCheckBody.put("orderBy", "0");
                        listCheckBody.put("inquiryType", "0");
                        Map<String, Object> listCheck = codefClient.callApi(
                                accessToken, "/v1/kr/bank/p/account/account-list", listCheckBody);
                        Map<String, Object> listCheckCode = (Map<String, Object>) listCheck.get("result");
                        if (listCheckCode == null || !"CF-00000".equals(listCheckCode.get("code"))) {
                            String msg = addResultCode != null
                                    ? (String) addResultCode.get("message") : "알 수 없는 오류";
                            throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "기관 추가 실패: " + msg);
                        }
                    }
                }
            }

            // 연동 기관 저장 (재연동으로 이미 등록된 기관이면 스킵)
            if (!institutionAlreadyRegistered) {
                CodefConnectedInstitutionDto instDto = new CodefConnectedInstitutionDto();
                instDto.setCodefConnectionId(existingConn.getId());
                instDto.setOrganizationCode(req.getOrganizationCode());
                instDto.setOrganizationName(req.getOrganizationName());
                instDto.setBusinessType(req.getBusinessType());
                assetMapper.insertConnectedInstitution(instDto);
            }

            // 계좌 목록 조회
            Map<String, Object> listBody = new HashMap<>();
            listBody.put("connectedId", connectedId);
            listBody.put("organization", req.getOrganizationCode());
            listBody.put("startDate", "19000101");
            listBody.put("endDate", "99991231");
            listBody.put("orderBy", "0");
            listBody.put("inquiryType", "0");

            Map<String, Object> listResult = codefClient.callApi(accessToken, "/v1/kr/bank/p/account/account-list", listBody);
            Map<String, Object> listResultCode = (Map<String, Object>) listResult.get("result");
            if (listResultCode == null || !"CF-00000".equals(listResultCode.get("code"))) {
                String msg = listResultCode != null ? (String) listResultCode.get("message") : "알 수 없는 오류";
                throw new CustomException(HttpStatus.BAD_REQUEST, "ACCOUNT_FETCH_FAIL", "계좌 목록 조회 실패: " + msg);
            }
            Map<String, Object> listData = (Map<String, Object>) listResult.get("data");
            List<Map<String, Object>> accountList = new ArrayList<>();
            for (String key : new String[]{"resDepositTrust"}) {
                Object raw = listData.get(key);
                if (raw instanceof List) {
                    accountList.addAll((List<Map<String, Object>>) raw);
                } else if (raw instanceof Map) {
                    accountList.add((Map<String, Object>) raw);
                }
            }

            if (accountList.isEmpty()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "ACCOUNT_NOT_FOUND", "연동 가능한 계좌가 없습니다.");
            }

            // accounts 테이블 저장 (재연동 시 중복 insert 방지 — 기존 계좌면 잔액 업데이트)
            List<AccountDto> saved = new ArrayList<>();
            for (Map<String, Object> acc : accountList) {
                AccountDto dto = new AccountDto();
                dto.setUserId(userId);
                dto.setCodefConnectionId(existingConn.getId());
                dto.setOrganizationCode(req.getOrganizationCode());
                dto.setAccountNumber((String) acc.get("resAccount"));
                dto.setAccountName((String) acc.getOrDefault("resAccountName", ""));
                dto.setAccountType(resolveAccountType((String) acc.get("resAccountKind")));
                dto.setBalance(parseBigDecimal(acc.get("resAccountBalance")));
                dto.setWithdrawableAmount(parseBigDecimal(acc.get("resWithdrawableAmount")));
                dto.setConnectionType("CODEF");

                AccountDto existing = assetMapper.findAccountByUserIdAndNumber(
                        userId, dto.getOrganizationCode(), dto.getAccountNumber());
                if (existing != null) {
                    assetMapper.updateAccountOnReconnect(dto);
                    dto.setId(existing.getId());
                } else {
                    assetMapper.insertAccount(dto);
                }
                assetMapper.linkCardsToAccountByPaymentNumber(userId, dto.getId(), dto.getAccountNumber());
                saved.add(dto);
            }

            return saved;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CODEF_ERROR", "계좌 연동 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public List<CardDto> linkCard(Long userId, CardLinkRequestDto req) {
        try {
            String encryptedPw = codefClient.encodePassword(req.getPassword());
            String accessToken = codefClient.getAccessToken();

            CodefConnectionDto conn = assetMapper.findConnectionByUserId(userId);
            String connectedId;
            String addFailMsg = null;

            if (conn == null) {
                Map<String, Object> account = new HashMap<>();
                account.put("countryCode", "KR");
                account.put("businessType", "CD");
                account.put("clientType", "P");
                account.put("organization", req.getOrganizationCode());
                account.put("loginType", req.getLoginType());
                account.put("id", req.getLoginId());
                account.put("password", encryptedPw);

                Map<String, Object> createBody = new HashMap<>();
                createBody.put("accountList", List.of(account));

                Map<String, Object> createResult = codefClient.callApi(accessToken, "/v1/account/create", createBody);
                Map<String, Object> resultCode = (Map<String, Object>) createResult.get("result");
                if (resultCode == null || !"CF-00000".equals(resultCode.get("code"))) {
                    String msg = resultCode != null ? (String) resultCode.get("message") : "알 수 없는 오류";
                    throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "카드 등록 실패: " + msg);
                }
                Map<String, Object> data = (Map<String, Object>) createResult.get("data");
                connectedId = (String) data.get("connectedId");

                CodefConnectionDto newConn = new CodefConnectionDto();
                newConn.setUserId(userId);
                newConn.setConnectedId(connectedId);
                assetMapper.insertCodefConnection(newConn);
                conn = assetMapper.findConnectionByUserId(userId);

                insertCardInstitutionIfAbsent(conn.getId(), req);
            } else {
                connectedId = conn.getConnectedId();

                CodefConnectedInstitutionDto existing =
                        assetMapper.findConnectedInstitution(conn.getId(), req.getOrganizationCode(), "CD");

                if (existing == null) {
                    Map<String, Object> addAccount = new HashMap<>();
                    addAccount.put("countryCode", "KR");
                    addAccount.put("businessType", "CD");
                    addAccount.put("clientType", "P");
                    addAccount.put("organization", req.getOrganizationCode());
                    addAccount.put("loginType", req.getLoginType());
                    addAccount.put("id", req.getLoginId());
                    addAccount.put("password", encryptedPw);

                    Map<String, Object> addBody = new HashMap<>();
                    addBody.put("connectedId", connectedId);
                    addBody.put("accountList", List.of(addAccount));

                    Map<String, Object> addResult = codefClient.callApi(accessToken, "/v1/account/add", addBody);
                    Map<String, Object> addResultCode = (Map<String, Object>) addResult.get("result");
                    if (addResultCode == null || !"CF-00000".equals(addResultCode.get("code"))) {
                        // CODEF 서버에는 이미 등록돼 있지만 DB 기록만 없는 불일치 상태일 수 있으므로
                        // 즉시 실패시키지 않고 카드 목록 조회 결과로 최종 판단한다.
                        addFailMsg = addResultCode != null ? (String) addResultCode.get("message") : "알 수 없는 오류";
                    } else {
                        insertCardInstitutionIfAbsent(conn.getId(), req);
                    }
                }
            }

            // 카드 목록 조회
            Map<String, Object> listBody = new HashMap<>();
            listBody.put("connectedId", connectedId);
            listBody.put("organization", req.getOrganizationCode());
            listBody.put("startDate", "19000101");
            listBody.put("endDate", "99991231");

            Map<String, Object> listResult = codefClient.callApi(accessToken, "/v1/kr/card/p/account/card-list", listBody);
            Map<String, Object> listResultCode = (Map<String, Object>) listResult.get("result");
            if (listResultCode == null || !"CF-00000".equals(listResultCode.get("code"))) {
                if (addFailMsg != null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "카드사 추가 실패: " + addFailMsg);
                }
                String msg = listResultCode != null ? (String) listResultCode.get("message") : "알 수 없는 오류";
                throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_FETCH_FAIL", "카드 목록 조회 실패: " + msg);
            }

            // add는 실패했지만 카드 목록 조회는 성공한 경우 = CODEF에는 이미 등록되어 있었다는 뜻이므로
            // 누락됐던 DB 기관 기록을 보완한다.
            if (addFailMsg != null) {
                insertCardInstitutionIfAbsent(conn.getId(), req);
            }

            Object listData = listResult.get("data");
            Object rawList;
            if (listData instanceof List) {
                // 개인 보유카드 API는 data 자체를 배열로 반환한다.
                rawList = listData;
            } else if (listData instanceof Map) {
                // 일부 기관/응답 버전은 resCardList 안에 카드 배열을 반환한다.
                rawList = ((Map<String, Object>) listData).get("resCardList");
                if (rawList == null && ((Map<?, ?>) listData).containsKey("resCardNo")) {
                    rawList = listData;
                }
            } else {
                rawList = null;
            }
            List<Map<String, Object>> cardList;
            if (rawList instanceof List) {
                cardList = (List<Map<String, Object>>) rawList;
            } else if (rawList instanceof Map) {
                cardList = List.of((Map<String, Object>) rawList);
            } else {
                cardList = Collections.emptyList();
            }

            if (cardList.isEmpty()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_NOT_FOUND", "연동 가능한 카드가 없습니다.");
            }

            List<CardDto> saved = new ArrayList<>();
            for (Map<String, Object> card : cardList) {
                CardDto dto = new CardDto();
                dto.setUserId(userId);
                dto.setCodefConnectionId(conn.getId());
                dto.setCardName((String) card.getOrDefault("resCardName", req.getOrganizationName() + " 카드"));
                dto.setMaskedCardNumber((String) card.get("resCardNo"));
                dto.setOrganizationCode(req.getOrganizationCode());
                dto.setPaymentAccountNumber((String) card.get("resPaymentAccount"));
                if (dto.getPaymentAccountNumber() != null && !dto.getPaymentAccountNumber().isBlank()) {
                    AccountDto paymentAccount = assetMapper.findAccountByUserIdAndNumberOnly(
                            userId, dto.getPaymentAccountNumber());
                    if (paymentAccount != null) {
                        dto.setLinkedAccountId(paymentAccount.getId());
                    }
                }
                String resCardType = String.valueOf(card.getOrDefault("resCardType", ""));
                dto.setCardType(
                        "02".equals(resCardType) || resCardType.contains("체크")
                                ? "CHECK"
                                : "CREDIT"
                );

                CardDto existingCard = assetMapper.findCardByUserIdAndNumber(userId, dto.getMaskedCardNumber());
                if (existingCard != null) {
                    dto.setId(existingCard.getId());
                    assetMapper.updateCardOnReconnect(dto);
                } else {
                    assetMapper.insertCard(dto);
                }
                // 트래블카드 상품과 이름이 일치할 때만 사용자 보유 트래블카드로 등록한다.
                // 이후 월렛 연결은 기존 화면에서 사용자가 직접 수행한다.
                assetMapper.upsertUserTravelCardFromLinkedCard(
                        userId,
                        dto.getCardName(),
                        dto.getMaskedCardNumber(),
                        dto.getOrganizationCode()
                );
                saved.add(dto);
            }

            return saved;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CODEF_ERROR", "카드 연동 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // connection_id + organization_code + businessType('CD') 기준으로 없을 때만 기관 기록을 추가한다.
    private void insertCardInstitutionIfAbsent(Long codefConnectionId, CardLinkRequestDto req) {
        CodefConnectedInstitutionDto existing =
                assetMapper.findConnectedInstitution(codefConnectionId, req.getOrganizationCode(), "CD");
        if (existing == null) {
            CodefConnectedInstitutionDto inst = new CodefConnectedInstitutionDto();
            inst.setCodefConnectionId(codefConnectionId);
            inst.setOrganizationCode(req.getOrganizationCode());
            inst.setOrganizationName(req.getOrganizationName());
            inst.setBusinessType("CD");
            assetMapper.insertConnectedInstitution(inst);
        }
    }

    /**
     * Codef 수시입출 거래내역 조회
     * 계좌 ID로 connectedId와 계좌번호를 찾아 Codef API를 호출하고 결과를 DB에 저장
     */

    @Transactional
    public List<TransactionDto> fetchTransactions(Long userId, TransactionRequestDto req)
    {
        try{
            String accessToken = codefClient.getAccessToken();

            //계좌 정보 조회(계좌번호 필요)
            AccountDto account = assetMapper.findAccountById(req.getAccountId(), userId);
            if (account == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다.");
            }

            //연동 정보 조회 (connectedId 필요)
            CodefConnectionDto conn = assetMapper.findConnectionByUserId(userId);
            if (conn == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "CONNECTION_NOT_FOUND", "연동 정보를 찾을 수 없습니다.");
            }

            //Codef 거래내역 조회 API 호출
            Map<String, Object> body = new HashMap<>();
            body.put("connectedId", conn.getConnectedId());
            body.put("organization", account.getOrganizationCode());
            body.put("account", account.getAccountNumber());
            body.put("startDate", req.getStartDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            body.put("endDate", req.getEndDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            body.put("orderBy", "0");
            body.put("inquiryType", "0");

            Map<String, Object> result = codefClient.callApi(accessToken, "/v1/kr/bank/p/account/transaction-list", body);
            Map<String, Object> resultCode = (Map<String, Object>) result.get("result");
            if (resultCode == null || !"CF-00000".equals(resultCode.get("code"))) {
                String msg = resultCode != null ? (String) resultCode.get("message") : "알 수 없는 오류";
                throw new CustomException(HttpStatus.BAD_REQUEST, "TRANSACTION_FETCH_FAIL", "거래내역 조회 실패: " + msg);
            }

            //거래내역 파싱 (1건이면 Map, 여러 건이면 List)
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            Object rawList = data.get("resTrHistoryList");
            List<Map<String, Object>> tranList;
            if (rawList instanceof List) {
                tranList = (List<Map<String, Object>>) rawList;
            } else if (rawList instanceof Map) {
                tranList = List.of((Map<String, Object>) rawList);
            } else {
                tranList = Collections.emptyList();
            }

            //거래내역 DB 저장
            List<TransactionDto> saved = new ArrayList<>();
            for (Map<String, Object> tran : tranList) {
                TransactionDto dto = new TransactionDto();
                String trDate = (String) tran.get("resAccountTrDate");
                String trTime = (String) tran.get("resAccountTrTime");
                BigDecimal resIn  = parseBigDecimal(tran.get("resAccountIn"));
                BigDecimal resOut = parseBigDecimal(tran.get("resAccountOut"));
                boolean isDeposit = resIn.compareTo(BigDecimal.ZERO) > 0;
                BigDecimal amount = isDeposit ? resIn : resOut;
                String desc2 = (String) tran.get("resAccountDesc2");
                String desc1 = (String) tran.get("resAccountDesc1");
                String merchantName = (desc2 != null && !desc2.isEmpty()) ? desc2 : desc1;
                dto.setAccountId(req.getAccountId());
                dto.setExternalKey("ACC:" + req.getAccountId() + ":" + trDate + ":" + trTime + ":" + amount.toPlainString());
                dto.setTransactionDate(LocalDate.parse(trDate, DateTimeFormatter.ofPattern("yyyyMMdd")));
                dto.setTransactionTime(LocalTime.parse(trTime, DateTimeFormatter.ofPattern("HHmmss")));
                dto.setTransactionType(isDeposit ? "DEPOSIT" : "WITHDRAWAL");
                dto.setTransactionRegion("DOMESTIC");
                dto.setAmount(amount);
                dto.setBalanceAfter(parseBigDecimal(tran.get("resAfterTranBalance")));
                dto.setMerchantName(merchantName);
                assetMapper.insertTransaction(dto);
                saved.add(dto);
            }

            tryAutoGenerateAnalysis(userId);

            return saved;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "TRANSACTION_ERROR", "거래내역 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public List<TransactionDto> fetchCardTransactions(Long userId, Long cardId, String startDate, String endDate) {
        try {
            String accessToken = codefClient.getAccessToken();

            CardDto card = assetMapper.findCardByIdAndUserId(cardId, userId);
            if (card == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "카드를 찾을 수 없습니다.");
            }

            CodefConnectionDto conn = assetMapper.findConnectionByUserId(userId);
            if (conn == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "CONNECTION_NOT_FOUND", "연동 정보를 찾을 수 없습니다.");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("connectedId", conn.getConnectedId());
            body.put("organization", card.getOrganizationCode());
            body.put("startDate", startDate.replace("-", ""));
            body.put("endDate", endDate.replace("-", ""));
            body.put("cardNo", card.getMaskedCardNumber());
            body.put("orderBy", "0");
            body.put("inquiryType", "0");
            body.put("memberStoreInfoType", "1");

            Map<String, Object> result = codefClient.callApi(accessToken, "/v1/kr/card/p/account/approval-list", body);
            Map<String, Object> resultCode = (Map<String, Object>) result.get("result");
            if (resultCode == null || !"CF-00000".equals(resultCode.get("code"))) {
                String msg = resultCode != null ? (String) resultCode.get("message") : "알 수 없는 오류";
                throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_TRANSACTION_FETCH_FAIL", "카드 거래내역 조회 실패: " + msg);
            }

            Object data = result.get("data");
            Object rawList;
            if (data instanceof List) {
                // 개인카드 승인내역 API는 data 자체를 배열로 반환할 수 있다.
                rawList = data;
            } else if (data instanceof Map) {
                rawList = ((Map<String, Object>) data).get("resApprovalList");
                if (rawList == null && ((Map<?, ?>) data).containsKey("resUsedDate")) {
                    rawList = data;
                }
            } else {
                rawList = null;
            }
            List<Map<String, Object>> approvalList;
            if (rawList instanceof List) {
                approvalList = (List<Map<String, Object>>) rawList;
            } else if (rawList instanceof Map) {
                approvalList = List.of((Map<String, Object>) rawList);
            } else {
                approvalList = Collections.emptyList();
            }

            List<TransactionDto> saved = new ArrayList<>();
            Map<String, Long> categoryIds = new HashMap<>();
            for (Map<String, Object> approval : approvalList) {
                String usedDate = (String) approval.get("resUsedDate");
                String usedTime = (String) approval.getOrDefault("resUsedTime", "000000");
                BigDecimal amount = parseBigDecimal(approval.get("resUsedAmount"));
                String cancelYN = (String) approval.getOrDefault("resCancelYN", "N");

                String approvalNo = (String) approval.get("resApprovalNo");
                String keySuffix = (approvalNo != null && !approvalNo.isBlank())
                        ? approvalNo
                        : usedDate + ":" + usedTime + ":" + amount.toPlainString();
                String externalKey = "CARD:" + cardId + ":" + keySuffix;

                if ("Y".equals(cancelYN)) {
                    // 이전에 저장된 동일 승인 건이 있다면 취소 처리로 논리 삭제한다.
                    assetMapper.deleteTransactionByExternalKey(externalKey);
                    continue;
                }

                TransactionDto dto = new TransactionDto();
                dto.setCardId(cardId);
                dto.setExternalKey(externalKey);
                dto.setTransactionDate(LocalDate.parse(usedDate, DateTimeFormatter.ofPattern("yyyyMMdd")));
                dto.setTransactionTime(LocalTime.parse(usedTime, DateTimeFormatter.ofPattern("HHmmss")));
                dto.setTransactionType("WITHDRAWAL");
                dto.setTransactionRegion("DOMESTIC");
                dto.setAmount(amount);
                String merchantName = (String) approval.get("resMemberStoreName");
                if (merchantName == null || merchantName.isBlank()) {
                    // 이전/기관별 응답 필드명도 함께 지원한다.
                    merchantName = (String) approval.get("resMerchantName");
                }
                dto.setMerchantName(merchantName);
                dto.setMerchantType((String) approval.get("resMemberStoreType"));
                applyAutomaticClassification(dto, categoryIds);
                assetMapper.upsertTransactionFromCard(dto);
                saved.add(dto);
            }

            assetMapper.updateCardLastSyncedAt(cardId);

            assetMapper.assignTripCountryToCardTransactions(userId);

            tryAutoGenerateAnalysis(userId);

            return saved;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_TRANSACTION_ERROR", "카드 거래내역 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public TransactionReclassificationResponseDto reclassifyCardTransactions(Long userId) {
        List<TransactionDto> targets = assetMapper.findUnclassifiedCardTransactionsByUserId(userId);
        Map<String, Long> categoryIds = new HashMap<>();
        int classifiedCount = 0;
        int fallbackCount = 0;
        int updatedCount = 0;

        for (TransactionDto target : targets) {
            CategoryClassificationResult classification =
                    applyAutomaticClassification(target, categoryIds);
            int updated = assetMapper.updateAutoClassification(target);
            updatedCount += updated;
            if (updated == 0) {
                continue;
            }

            if (classification.source() == CategorySource.FALLBACK) {
                fallbackCount++;
            } else {
                classifiedCount++;
            }
        }

        return new TransactionReclassificationResponseDto(
                targets.size(),
                classifiedCount,
                fallbackCount,
                updatedCount
        );
    }

    private CategoryClassificationResult applyAutomaticClassification(
            TransactionDto transaction,
            Map<String, Long> categoryIds
    ) {
        CategoryClassificationResult classification = transactionCategoryClassifier.classify(
                transaction.getMerchantName(),
                transaction.getMerchantType()
        );
        String categoryCode = classification.categoryCode().name();
        Long categoryId = categoryIds.computeIfAbsent(
                categoryCode,
                assetMapper::findCategoryIdByCode
        );
        if (categoryId == null) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CATEGORY_NOT_FOUND",
                    "소비 카테고리를 찾을 수 없습니다: " + categoryCode
            );
        }

        transaction.setCategoryId(categoryId);
        transaction.setCategorySource(classification.source().name());
        transaction.setCategoryConfidence(classification.confidence());
        transaction.setCategoryClassifiedAt(java.time.LocalDateTime.now());
        return classification;
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        AccountDto account = assetMapper.findAccountById(accountId, userId);
        if (account == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다.");
        }
        assetMapper.deleteAccount(accountId, userId);
    }

    @Transactional
    public void updateTransaction(Long userId, Long transactionId, TransactionUpdateRequestDto req ){
        TransactionDto transaction = assetMapper.findTransactionById(transactionId, userId);
        if(transaction == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "거래내역을 찾을 수 없습니다.");
        }
        assetMapper.updateTransaction(transactionId, userId, req.getCategoryId(), req.getMemo());
    }


    public List<AccountDto> getAccounts(Long userId) {
        return assetMapper.findAccountsByUserId(userId);
    }

    public List<CardDto> getCards(Long userId) {
        return assetMapper.findCardsByUserId(userId);
    }

    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        CardDto card = assetMapper.findCardByIdAndUserId(cardId, userId);
        if (card == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "카드를 찾을 수 없습니다.");
        }
        assetMapper.deleteCard(cardId, userId);
        if (card.getMaskedCardNumber() != null) {
            assetMapper.deleteUserTravelCardByCard(userId, card.getMaskedCardNumber());
        }
    }

    public List<TransactionDto> getCardTransactions(Long userId, Long cardId, String startDate, String endDate) {
        CardDto card = assetMapper.findCardByIdAndUserId(cardId, userId);
        if (card == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "카드를 찾을 수 없습니다.");
        }
        LocalDate start = parseDateOrNull(startDate);
        LocalDate end = parseDateOrNull(endDate);
        return assetMapper.findTransactionsByCardId(cardId, start, end);
    }

    // yyyy-MM-dd 형식이 아닌 값은 CustomException(400)으로 변환한다 (그대로 두면 DateTimeParseException이 500으로 응답됨).
    private LocalDate parseDateOrNull(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_DATE_FORMAT", "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    public AccountTransactionResponseDto getAccountTransactions(
            Long userId, Long accountId, LocalDate startDate, LocalDate endDate, String type) {
        AccountDto account = assetMapper.findAccountById(accountId, userId);
        if (account == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다.");
        }
        List<TransactionDto> transactions = assetMapper.findTransactionsByAccountIdWithFilter(
                accountId, startDate, endDate, type);
        AccountTransactionResponseDto response = new AccountTransactionResponseDto();
        response.setAccountName(account.getAccountName());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountType(account.getAccountType());
        response.setBalance(account.getBalance());
        response.setTransactions(transactions);
        return response;
    }

    public List<TransactionDto> getAllTransactions(Long userId, String startDate, String endDate) {
        LocalDate start = parseDateOrNull(startDate);
        LocalDate end = parseDateOrNull(endDate);
        List<TransactionDto> transactions = assetMapper.findTransactionsByUserId(userId, start, end);
        List<TransactionDto> accountWithdrawals = transactions.stream()
                .filter(transaction -> transaction.getAccountId() != null)
                .filter(transaction -> "WITHDRAWAL".equals(transaction.getTransactionType()))
                .toList();
        List<TransactionDto> checkCardWithdrawals = transactions.stream()
                .filter(transaction -> transaction.getCardId() != null)
                .filter(transaction -> "CHECK".equals(transaction.getSourceCardType()))
                .filter(transaction -> "WITHDRAWAL".equals(transaction.getTransactionType()))
                .toList();
        Set<Long> duplicateAccountIds = duplicateTransactionMatcher
                .findDuplicateAccountTransactionIds(accountWithdrawals, checkCardWithdrawals);

        return transactions.stream()
                .filter(transaction -> !duplicateAccountIds.contains(transaction.getId()))
                .toList();
    }

    public TransactionDto getTransactionDetail(Long userId, Long transactionId) {
        TransactionDto transaction = assetMapper.findTransactionById(transactionId, userId);
        if (transaction == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "거래내역을 찾을 수 없습니다.");
        }
        return transaction;
    }

    public List<SupportedInstitutionDto> getSupportedBankInstitutions() {
        return assetMapper.findSupportedInstitutionsByBusinessType("BK");
    }

    public List<SupportedInstitutionDto> getSupportedCardInstitutions() {
        return assetMapper.findSupportedInstitutionsByBusinessType("CD");
    }

    public List<CalendarDayDto> getCalendar(Long userId, Integer year, Integer month, String type) {
        if (year == null) year = java.time.LocalDate.now().getYear();
        if (month == null) month = java.time.LocalDate.now().getMonthValue();

        if (month < 1 || month > 12) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_MONTH", "월은 1~12 사이여야 합니다.");
        }

        if (type != null && (type.isEmpty() || type.equals("ALL"))) {
            type = null;
        }

        return assetMapper.findCalendarByMonth(userId, year, month, type);
    }

    private void tryAutoGenerateAnalysis(Long userId) {
        try {
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            monthlySpendingAnalysisService.generateMonthlyAnalysis(userId, previousMonth);
            log.info("신규 사용자 자동 분석 리포트 생성 완료 - userId: {}, 분석월: {}", userId, previousMonth);
        } catch (Exception e) {
            log.warn("자동 분석 리포트 생성 실패 (거래 동기화는 정상) - userId: {}, 사유: {}", userId, e.getMessage());
        }
    }

    private String resolveAccountType(String resAccountKind) {
        if (resAccountKind == null) return "CHECKING";
        return switch (resAccountKind) {
            case "10" -> "CHECKING";   // 입출금
            case "20" -> "DEPOSIT";    // 예금
            case "30" -> "SAVING";     // 적금
            default   -> "CHECKING";
        };
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.toString().replaceAll(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
