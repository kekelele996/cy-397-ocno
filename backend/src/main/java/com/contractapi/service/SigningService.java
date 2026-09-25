package com.contractapi.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.ErrorCode;
import com.contractapi.constants.InvitationStatus;
import com.contractapi.constants.SignRecordStatus;
import com.contractapi.dto.CreateInvitationRequest;
import com.contractapi.dto.InvitationProgressResponse;
import com.contractapi.dto.SignRequest;
import com.contractapi.entity.Contract;
import com.contractapi.entity.SignInvitation;
import com.contractapi.entity.SignRecord;
import com.contractapi.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SigningService {
  private static final Logger log = LoggerFactory.getLogger(SigningService.class);

  private final ContractService contractService;
  private final List<SignInvitation> invitations = new ArrayList<>();
  private final List<SignRecord> records = new ArrayList<>();
  private long sequence = 0;

  public SigningService(ContractService contractService) {
    this.contractService = contractService;
  }

  public synchronized SignInvitation createInvitation(Long contractId, CreateInvitationRequest request) {
    Contract contract = contractService.getContract(contractId);
    if (!ContractStatus.DRAFT.name().equals(contract.getStatus())) {
      throw new ApiException(ErrorCode.INVALID_CONTRACT_STATE, "仅草稿状态的合同可以发起签署邀请");
    }
    if (request.signers() == null || request.signers().isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署方名单不能为空");
    }
    Set<String> signers = new LinkedHashSet<>();
    for (String signer : request.signers()) {
      if (signer == null || signer.isBlank()) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署方身份不能为空");
      }
      if (!signers.add(signer)) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署方重复: " + signer);
      }
    }
    LocalDateTime now = LocalDateTime.now();
    if (request.deadline() == null || !request.deadline().isAfter(now)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "截止时间必须晚于当前时间");
    }

    SignInvitation invitation = new SignInvitation();
    invitation.setId(nextId());
    invitation.setContractId(contract.getId());
    invitation.setDeadline(request.deadline());
    invitation.setStatus(InvitationStatus.ACTIVE.name());
    invitation.setCreatedAt(now);
    invitations.add(invitation);

    for (String signer : signers) {
      SignRecord record = new SignRecord();
      record.setId(nextId());
      record.setInvitationId(invitation.getId());
      record.setContractId(contract.getId());
      record.setSigner(signer);
      record.setStatus(SignRecordStatus.PENDING.name());
      records.add(record);
    }

    contract.setStatus(ContractStatus.PENDING_SIGN.name());
    log.info("合同 {} 发起签署邀请 {}，签署方 {} 人，截止 {}", contract.getId(), invitation.getId(), signers.size(), request.deadline());
    return invitation;
  }

  public synchronized SignRecord sign(Long contractId, SignRequest request) {
    Contract contract = contractService.getContract(contractId);
    SignInvitation invitation = findInvitation(contractId);
    if (invitation == null) {
      throw new ApiException(ErrorCode.INVALID_CONTRACT_STATE, "合同尚未发起签署邀请");
    }
    if (request.signer() == null || request.signer().isBlank()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署方身份不能为空");
    }
    refreshExpiration(contract, invitation);

    SignRecord record = findRecord(invitation.getId(), request.signer());
    if (record != null && SignRecordStatus.SIGNED.name().equals(record.getStatus())) {
      // 幂等：同一人重复提交，仍返回第一次的签署结果
      return record;
    }
    if (!InvitationStatus.ACTIVE.name().equals(invitation.getStatus())) {
      // 已过期或已完成：之后到达的签名不改写合同或已有签署记录
      throw new ApiException(ErrorCode.INVITATION_EXPIRED, "签署邀请已关闭，签名不再受理");
    }
    if (record == null) {
      throw new ApiException(ErrorCode.SIGNER_NOT_INVITED, "签署方不在邀请名单中: " + request.signer());
    }

    LocalDateTime now = LocalDateTime.now();
    record.setStatus(SignRecordStatus.SIGNED.name());
    record.setSignedAt(now);
    log.info("合同 {} 签署方 {} 完成签署", contract.getId(), request.signer());

    boolean allSigned = recordsOf(invitation.getId()).stream()
        .allMatch(item -> SignRecordStatus.SIGNED.name().equals(item.getStatus()));
    if (allSigned) {
      invitation.setStatus(InvitationStatus.COMPLETED.name());
      contract.setStatus(ContractStatus.SIGNED.name());
      contract.setSignedAt(now);
      log.info("合同 {} 全部签署方已完成，合同状态置为已签署", contract.getId());
    }
    return record;
  }

  public synchronized InvitationProgressResponse progress(Long contractId) {
    Contract contract = contractService.getContract(contractId);
    SignInvitation invitation = findInvitation(contractId);
    if (invitation == null) {
      throw new ApiException(ErrorCode.NOT_FOUND, "合同尚未发起签署邀请: " + contractId);
    }
    refreshExpiration(contract, invitation);

    List<SignRecord> signers = recordsOf(invitation.getId());
    List<InvitationProgressResponse.SignerResult> results = signers.stream()
        .map(item -> new InvitationProgressResponse.SignerResult(item.getSigner(), item.getStatus(), item.getSignedAt()))
        .toList();
    int signed = (int) signers.stream().filter(item -> SignRecordStatus.SIGNED.name().equals(item.getStatus())).count();
    return new InvitationProgressResponse(contract.getId(), contract.getStatus(), invitation.getId(),
        invitation.getStatus(), invitation.getDeadline(), signers.size(), signed, results);
  }

  private void refreshExpiration(Contract contract, SignInvitation invitation) {
    if (!InvitationStatus.ACTIVE.name().equals(invitation.getStatus())) {
      return;
    }
    if (LocalDateTime.now().isBefore(invitation.getDeadline())) {
      return;
    }
    invitation.setStatus(InvitationStatus.EXPIRED.name());
    contract.setStatus(ContractStatus.EXPIRED.name());
    recordsOf(invitation.getId()).stream()
        .filter(item -> SignRecordStatus.PENDING.name().equals(item.getStatus()))
        .forEach(item -> item.setStatus(SignRecordStatus.EXPIRED.name()));
    log.info("合同 {} 签署邀请 {} 已过期，合同状态置为已过期", contract.getId(), invitation.getId());
  }

  private SignInvitation findInvitation(Long contractId) {
    return invitations.stream().filter(item -> item.getContractId().equals(contractId)).findFirst().orElse(null);
  }

  private SignRecord findRecord(Long invitationId, String signer) {
    return records.stream()
        .filter(item -> item.getInvitationId().equals(invitationId) && item.getSigner().equals(signer))
        .findFirst().orElse(null);
  }

  private List<SignRecord> recordsOf(Long invitationId) {
    return records.stream().filter(item -> item.getInvitationId().equals(invitationId)).toList();
  }

  private long nextId() {
    return ++sequence;
  }
}
