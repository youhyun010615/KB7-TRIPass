package com.tripass.asset.service;

import com.tripass.asset.dto.*;
import com.tripass.asset.mapper.AssetMapper;
import com.tripass.common.exception.CustomException;
import com.tripass.common.util.CodefUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetMapper assetMapper;

    @Value("${codef.client-id}")
    private String clientId;

    @Value("${codef.client-secret}")
    private String clientSecret;

    public AssetService(AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Transactional
    public List<AccountDto> linkBank(Long userId, CodefLinkRequestDto req) {
        try {
            String encryptedPw = CodefUtil.encryptRSA(req.getPassword());
            String accessToken = CodefUtil.getAccessToken(clientId, clientSecret);

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

                Map<String, Object> createResult = CodefUtil.callApi(accessToken, "/v1/account/create", createBody);
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

                Map<String, Object> addResult = CodefUtil.callApi(accessToken, "/v1/account/add", addBody);
                Map<String, Object> addResultCode = (Map<String, Object>) addResult.get("result");
                if (addResultCode == null || !"CF-00000".equals(addResultCode.get("code"))) {
                    // add 실패 시 기관이 이미 등록된 상태인지 계좌 목록 조회로 확인
                    Map<String, Object> listCheckBody = new HashMap<>();
                    listCheckBody.put("connectedId", connectedId);
                    listCheckBody.put("organization", req.getOrganizationCode());
                    listCheckBody.put("startDate", "19000101");
                    listCheckBody.put("endDate", "99991231");
                    listCheckBody.put("orderBy", "0");
                    listCheckBody.put("inquiryType", "0");
                    Map<String, Object> listCheck = CodefUtil.callApi(accessToken, "/v1/kr/bank/p/account/account-list", listCheckBody);
                    Map<String, Object> listCheckCode = (Map<String, Object>) listCheck.get("result");
                    if (listCheckCode == null || !"CF-00000".equals(listCheckCode.get("code"))) {
                        String msg = addResultCode != null ? (String) addResultCode.get("message") : "알 수 없는 오류";
                        throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "기관 추가 실패: " + msg);
                    }
                    // 계좌 조회 성공 → 기관이 이미 Codef에 등록된 상태, institution 중복 insert 생략
                    institutionAlreadyRegistered = true;
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

            Map<String, Object> listResult = CodefUtil.callApi(accessToken, "/v1/kr/bank/p/account/account-list", listBody);
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
                dto.setAccountNumber((String) acc.get("resAccount"));
                dto.setAccountName((String) acc.getOrDefault("resAccountName", ""));
                dto.setAccountType(resolveAccountType((String) acc.get("resAccountKind")));
                dto.setBalance(parseBigDecimal(acc.get("resAccountBalance")));
                dto.setWithdrawableAmount(parseBigDecimal(acc.get("resWithdrawableAmount")));
                dto.setConnectionType("CODEF");

                AccountDto existing = assetMapper.findAccountByUserIdAndNumber(userId, dto.getAccountNumber());
                if (existing != null) {
                    assetMapper.updateAccountOnReconnect(dto);
                    dto.setId(existing.getId());
                } else {
                    assetMapper.insertAccount(dto);
                }
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
            String encryptedPw = CodefUtil.encryptRSA(req.getPassword());
            String accessToken = CodefUtil.getAccessToken(clientId, clientSecret);

            CodefConnectionDto conn = assetMapper.findConnectionByUserId(userId);
            String connectedId;

            if (conn == null) {
                Map<String, Object> account = new HashMap<>();
                account.put("countryCode", "KR");
                account.put("businessType", "CF");
                account.put("clientType", "P");
                account.put("organization", req.getOrganizationCode());
                account.put("loginType", req.getLoginType());
                account.put("id", req.getLoginId());
                account.put("password", encryptedPw);

                Map<String, Object> createBody = new HashMap<>();
                createBody.put("accountList", List.of(account));

                Map<String, Object> createResult = CodefUtil.callApi(accessToken, "/v1/account/create", createBody);
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
            } else {
                connectedId = conn.getConnectedId();

                Map<String, Object> addAccount = new HashMap<>();
                addAccount.put("countryCode", "KR");
                addAccount.put("businessType", "CF");
                addAccount.put("clientType", "P");
                addAccount.put("organization", req.getOrganizationCode());
                addAccount.put("loginType", req.getLoginType());
                addAccount.put("id", req.getLoginId());
                addAccount.put("password", encryptedPw);

                Map<String, Object> addBody = new HashMap<>();
                addBody.put("connectedId", connectedId);
                addBody.put("accountList", List.of(addAccount));

                Map<String, Object> addResult = CodefUtil.callApi(accessToken, "/v1/account/add", addBody);
                Map<String, Object> addResultCode = (Map<String, Object>) addResult.get("result");
                if (addResultCode == null || !"CF-00000".equals(addResultCode.get("code"))) {
                    String msg = addResultCode != null ? (String) addResultCode.get("message") : "알 수 없는 오류";
                    throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "카드사 추가 실패: " + msg);
                }
            }

            // 카드 목록 조회
            Map<String, Object> listBody = new HashMap<>();
            listBody.put("connectedId", connectedId);
            listBody.put("organization", req.getOrganizationCode());
            listBody.put("startDate", "19000101");
            listBody.put("endDate", "99991231");

            Map<String, Object> listResult = CodefUtil.callApi(accessToken, "/v1/kr/card/p/account/card-list", listBody);
            Map<String, Object> listResultCode = (Map<String, Object>) listResult.get("result");
            if (listResultCode == null || !"CF-00000".equals(listResultCode.get("code"))) {
                String msg = listResultCode != null ? (String) listResultCode.get("message") : "알 수 없는 오류";
                throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_FETCH_FAIL", "카드 목록 조회 실패: " + msg);
            }

            Map<String, Object> listData = (Map<String, Object>) listResult.get("data");
            Object rawList = listData.get("resCardList");
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
                dto.setCardType("02".equals(card.get("resCardType")) ? "CHECK" : "CREDIT");
                assetMapper.insertCard(dto);
                saved.add(dto);
            }

            return saved;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CODEF_ERROR", "카드 연동 중 오류가 발생했습니다: " + e.getMessage());
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
            String accessToken = CodefUtil.getAccessToken(clientId, clientSecret);

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

            Map<String, Object> result = CodefUtil.callApi(accessToken, "/v1/kr/bank/p/account/transaction-list", body);
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
                dto.setExternalKey(userId + ":" + trDate + ":" + trTime + ":" + amount.toPlainString());
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
            String accessToken = CodefUtil.getAccessToken(clientId, clientSecret);

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
            body.put("orderBy", "0");

            Map<String, Object> result = CodefUtil.callApi(accessToken, "/v1/kr/card/p/account/approval-list", body);
            Map<String, Object> resultCode = (Map<String, Object>) result.get("result");
            if (resultCode == null || !"CF-00000".equals(resultCode.get("code"))) {
                String msg = resultCode != null ? (String) resultCode.get("message") : "알 수 없는 오류";
                throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_TRANSACTION_FETCH_FAIL", "카드 거래내역 조회 실패: " + msg);
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            Object rawList = data.get("resApprovalList");
            List<Map<String, Object>> approvalList;
            if (rawList instanceof List) {
                approvalList = (List<Map<String, Object>>) rawList;
            } else if (rawList instanceof Map) {
                approvalList = List.of((Map<String, Object>) rawList);
            } else {
                approvalList = Collections.emptyList();
            }

            List<TransactionDto> saved = new ArrayList<>();
            for (Map<String, Object> approval : approvalList) {
                String usedDate = (String) approval.get("resUsedDate");
                String usedTime = (String) approval.getOrDefault("resUsedTime", "000000");
                BigDecimal amount = parseBigDecimal(approval.get("resUsedAmount"));
                String cancelYN = (String) approval.getOrDefault("resCancelYN", "N");

                if ("Y".equals(cancelYN)) continue;

                TransactionDto dto = new TransactionDto();
                dto.setCardId(cardId);
                dto.setExternalKey(userId + ":" + usedDate + ":" + usedTime + ":" + amount.toPlainString());
                dto.setTransactionDate(LocalDate.parse(usedDate, DateTimeFormatter.ofPattern("yyyyMMdd")));
                dto.setTransactionTime(LocalTime.parse(usedTime, DateTimeFormatter.ofPattern("HHmmss")));
                dto.setTransactionType("WITHDRAWAL");
                dto.setTransactionRegion("DOMESTIC");
                dto.setAmount(amount);
                dto.setMerchantName((String) approval.get("resMerchantName"));
                assetMapper.upsertTransactionFromCard(dto);
                saved.add(dto);
            }

            return saved;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_TRANSACTION_ERROR", "카드 거래내역 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
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

    public AccountTransactionResponseDto getAccountTransactions(
            Long userId, Long accountId, LocalDate startDate, LocalDate endDate, String type) {
        AccountDto account = assetMapper.findAccountById(accountId, userId);
        if (account == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다.");
        }
        List<TransactionDto> transactions = assetMapper.findTransactionsByAccountIdWithFilter(
                accountId, startDate, endDate, type);
        AccountTransactionResponseDto response = new AccountTransactionResponseDto();
        response.setBalance(account.getBalance());
        response.setTransactions(transactions);
        return response;
    }

    public List<TransactionDto> getAllTransactions(Long userId, String startDate, String endDate) {
        LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : null;
        return assetMapper.findTransactionsByUserId(userId, start, end);
    }

    public TransactionDto getTransactionDetail(Long userId, Long transactionId) {
        TransactionDto transaction = assetMapper.findTransactionById(transactionId, userId);
        if (transaction == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "거래내역을 찾을 수 없습니다.");
        }
        return transaction;
    }

    public List<SupportedInstitutionDto> getSupportedInstitutions() {
        return assetMapper.findAllSupportedInstitutions();
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