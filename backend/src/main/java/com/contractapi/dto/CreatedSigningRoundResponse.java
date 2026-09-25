package com.contractapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreatedSigningRoundResponse(Long id, Long contractId, Integer roundNumber, String status,
    String contractStatus, LocalDateTime expiresAt, LocalDateTime completedAt, LocalDateTime createdAt,
    int totalSigners, int signedSigners, List<CreatedSignerResponse> signers) {
  public record CreatedSignerResponse(Long id, String signerName, String signerEmail, String identityLastFour,
      String invitationToken, String status, LocalDateTime invitedAt) {}
}
