package com.contractapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("contract_signing_rounds")
public class ContractSigningRound {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long contractId;
  private Integer roundNumber;
  private String status;
  private LocalDateTime expiresAt;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getContractId() { return contractId; }
  public void setContractId(Long contractId) { this.contractId = contractId; }
  public Integer getRoundNumber() { return roundNumber; }
  public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
