package com.contractapi.constants;

public final class ErrorCode {
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String PDF_EXPORT_FAILED = "PDF_EXPORT_FAILED";
  public static final String ACTIVE_SIGNING_ROUND_EXISTS = "ACTIVE_SIGNING_ROUND_EXISTS";
  public static final String SIGNING_ROUND_NOT_FOUND = "SIGNING_ROUND_NOT_FOUND";
  public static final String INVITATION_NOT_FOUND = "INVITATION_NOT_FOUND";
  public static final String INVITATION_EXPIRED = "INVITATION_EXPIRED";
  public static final String IDENTITY_MISMATCH = "IDENTITY_MISMATCH";
  public static final String INVALID_SIGNING_STATE = "INVALID_SIGNING_STATE";
  private ErrorCode() {}
}
