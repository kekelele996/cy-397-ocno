package com.contractapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreateSigningRoundRequest(LocalDateTime expiresAt, List<SignerRequest> signers) {
  public record SignerRequest(String name, String email, String identityNumber) {}
}
