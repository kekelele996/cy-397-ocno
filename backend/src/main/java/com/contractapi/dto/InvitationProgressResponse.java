package com.contractapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InvitationProgressResponse(Long contractId, String contractStatus, Long invitationId,
    String invitationStatus, LocalDateTime deadline, int total, int signed, List<SignerResult> signers) {
  public record SignerResult(String signer, String status, LocalDateTime signedAt) {}
}
