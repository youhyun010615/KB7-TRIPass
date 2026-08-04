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
                    String msg = addResultCode != null ? (String) addResultCode.get("message") : "알 수 없는 오류";
                    throw new CustomException(HttpStatus.BAD_REQUEST, "CODEF_LINK_FAIL", "기관 추가 실패: " + msg);
                }
            }

            // 연동 기관 저장
            CodefConnectedInstitutionDto instDto = new CodefConnectedInstitutionDto();
            instDto.setCodefConnectionId(existingConn.getId());
            instDto.setOrganizationCode(req.getOrganizationCode());
            instDto.setBusinessType(req.getBusinessType());
            assetMapper.insertConnectedInstitution(instDto);

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

            // accounts 테이블 저장
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
                assetMapper.insertAccount(dto);
                saved.add(dto);
            }

            return saved;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CODEF_ERROR", "계좌 연동 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public List<AccountDto> getAccounts(Long userId) {
        return assetMapper.findAccountsByUserId(userId);
    }

    public List<SupportedInstitutionDto> getSupportedInstitutions() {
        return assetMapper.findAllSupportedInstitutions();
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