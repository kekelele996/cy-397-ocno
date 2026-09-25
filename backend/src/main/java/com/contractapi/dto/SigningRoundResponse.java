package com.contractapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SigningRoundResponse(Long id, Long contractId, Integer roundNumber, String status,
    String contractStatus, LocalDateTime expiresAt, LocalDateTime completedAt, LocalDateTime createdAt,
    int totalSigners, int signedSigners, List<SignerInvitationResponse> signers) {}
