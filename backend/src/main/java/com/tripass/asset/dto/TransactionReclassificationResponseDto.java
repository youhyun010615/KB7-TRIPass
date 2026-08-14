package com.tripass.asset.dto;

public record TransactionReclassificationResponseDto(
        int processedCount,
        int classifiedCount,
        int fallbackCount,
        int updatedCount
) {
}
