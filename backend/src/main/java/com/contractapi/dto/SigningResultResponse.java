package com.contractapi.dto;

import java.time.LocalDateTime;

public record SigningResultResponse(String roundStatus, String contractStatus, String signerName,
    String signerStatus, LocalDateTime firstSignedAt, String signatureValue, boolean firstSubmission) {}
