package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodefConnectionDto {
    private Long id;
    private Long userId;
    private String connectedId;
    private String connectionStatus;
    private String lastSyncedAt;
}
