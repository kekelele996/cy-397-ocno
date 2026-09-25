package com.contractapi.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sign_invitations")
public class SignInvitation {
  private Long id;
  private Long contractId;
  private LocalDateTime deadline;
  private String status;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getContractId() { return contractId; }
  public void setContractId(Long contractId) { this.contractId = contractId; }
  public LocalDateTime getDeadline() { return deadline; }
  public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
