package com.contractapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("contract_signer_invitations")
public class ContractSignerInvitation {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long roundId;
  private Long contractId;
  private String signerName;
  private String signerEmail;
  private String identityHash;
  private String identityLastFour;
  private String invitationToken;
  private String status;
  private LocalDateTime invitedAt;
  private LocalDateTime firstSignedAt;
  private String signatureValue;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getRoundId() { return roundId; }
  public void setRoundId(Long roundId) { this.roundId = roundId; }
  public Long getContractId() { return contractId; }
  public void setContractId(Long contractId) { this.contractId = contractId; }
  public String getSignerName() { return signerName; }
  public void setSignerName(String signerName) { this.signerName = signerName; }
  public String getSignerEmail() { return signerEmail; }
  public void setSignerEmail(String signerEmail) { this.signerEmail = signerEmail; }
  public String getIdentityHash() { return identityHash; }
  public void setIdentityHash(String identityHash) { this.identityHash = identityHash; }
  public String getIdentityLastFour() { return identityLastFour; }
  public void setIdentityLastFour(String identityLastFour) { this.identityLastFour = identityLastFour; }
  public String getInvitationToken() { return invitationToken; }
  public void setInvitationToken(String invitationToken) { this.invitationToken = invitationToken; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getInvitedAt() { return invitedAt; }
  public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
  public LocalDateTime getFirstSignedAt() { return firstSignedAt; }
  public void setFirstSignedAt(LocalDateTime firstSignedAt) { this.firstSignedAt = firstSignedAt; }
  public String getSignatureValue() { return signatureValue; }
  public void setSignatureValue(String signatureValue) { this.signatureValue = signatureValue; }
}
