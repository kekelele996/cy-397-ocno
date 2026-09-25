package com.contractapi.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sign_records")
public class SignRecord {
  private Long id;
  private Long invitationId;
  private Long contractId;
  private String signer;
  private String status;
  private LocalDateTime signedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getInvitationId() { return invitationId; }
  public void setInvitationId(Long invitationId) { this.invitationId = invitationId; }
  public Long getContractId() { return contractId; }
  public void setContractId(Long contractId) { this.contractId = contractId; }
  public String getSigner() { return signer; }
  public void setSigner(String signer) { this.signer = signer; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getSignedAt() { return signedAt; }
  public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
}
