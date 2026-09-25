package com.contractapi.dto;

public record SignContractRequest(String signerName, String identityNumber, String signatureValue) {}
