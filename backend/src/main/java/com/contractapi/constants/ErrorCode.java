package com.contractapi.constants;

public final class ErrorCode {
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String PDF_EXPORT_FAILED = "PDF_EXPORT_FAILED";
  public static final String INVALID_CONTRACT_STATE = "INVALID_CONTRACT_STATE";
  public static final String INVITATION_EXPIRED = "INVITATION_EXPIRED";
  public static final String SIGNER_NOT_INVITED = "SIGNER_NOT_INVITED";
  private ErrorCode() {}
}
