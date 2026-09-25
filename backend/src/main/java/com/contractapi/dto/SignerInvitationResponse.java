package com.contractapi.dto;

import java.time.LocalDateTime;

public record SignerInvitationResponse(Long id, String signerName, String signerEmail, String identityLastFour,
    String status, LocalDateTime invitedAt, LocalDateTime firstSignedAt, String signatureValue) {}
