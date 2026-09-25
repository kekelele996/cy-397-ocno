package com.contractapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.ErrorCode;
import com.contractapi.constants.SignerInvitationStatus;
import com.contractapi.constants.SigningRoundStatus;
import com.contractapi.dto.CreateSigningRoundRequest;
import com.contractapi.dto.CreatedSigningRoundResponse;
import com.contractapi.dto.SignContractRequest;
import com.contractapi.dto.SignerInvitationResponse;
import com.contractapi.dto.SigningResultResponse;
import com.contractapi.dto.SigningRoundResponse;
import com.contractapi.entity.Contract;
import com.contractapi.entity.ContractSignerInvitation;
import com.contractapi.entity.ContractSigningRound;
import com.contractapi.exception.ApiException;
import com.contractapi.mapper.ContractMapper;
import com.contractapi.mapper.ContractSignerInvitationMapper;
import com.contractapi.mapper.ContractSigningRoundMapper;
import com.contractapi.utils.IdentityHasher;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SigningService {
  private final ContractMapper contractMapper;
  private final ContractSigningRoundMapper roundMapper;
  private final ContractSignerInvitationMapper invitationMapper;
  private final SigningExpiryService expiryService;
  private final IdentityHasher identityHasher;

  public SigningService(ContractMapper contractMapper, ContractSigningRoundMapper roundMapper,
      ContractSignerInvitationMapper invitationMapper, SigningExpiryService expiryService,
      IdentityHasher identityHasher) {
    this.contractMapper = contractMapper;
    this.roundMapper = roundMapper;
    this.invitationMapper = invitationMapper;
    this.expiryService = expiryService;
    this.identityHasher = identityHasher;
  }

  @Transactional
  public CreatedSigningRoundResponse createRound(Long contractId, CreateSigningRoundRequest request) {
    if (request == null || request.expiresAt() == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署截止时间不能为空");
    }
    if (!request.expiresAt().isAfter(LocalDateTime.now())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署截止时间必须晚于当前时间");
    }
    if (request.signers() == null || request.signers().isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "至少需要一位签署方");
    }

    Contract contract = contractMapper.selectByIdForUpdate(contractId);
    if (contract == null) {
      throw new ApiException(ErrorCode.NOT_FOUND, "合同不存在");
    }
    if (ContractStatus.SIGNED.name().equals(contract.getStatus()) || ContractStatus.EXPIRED.name().equals(contract.getStatus())) {
      throw new ApiException(ErrorCode.INVALID_SIGNING_STATE, "已签署或已过期合同不能重新发起签署");
    }
    expiryService.expireContractRoundIfDue(contractId);
    Long activeRounds = roundMapper.selectCount(new LambdaQueryWrapper<ContractSigningRound>()
        .eq(ContractSigningRound::getContractId, contractId)
        .eq(ContractSigningRound::getStatus, SigningRoundStatus.OPEN.name()));
    if (activeRounds > 0) {
      throw new ApiException(ErrorCode.ACTIVE_SIGNING_ROUND_EXISTS, "该合同已有一轮进行中的签署邀请");
    }

    ContractSigningRound latestRound = roundMapper.selectOne(new LambdaQueryWrapper<ContractSigningRound>()
        .eq(ContractSigningRound::getContractId, contractId)
        .orderByDesc(ContractSigningRound::getRoundNumber)
        .last("LIMIT 1"));
    ContractSigningRound round = new ContractSigningRound();
    round.setContractId(contractId);
    round.setRoundNumber(latestRound == null ? 1 : latestRound.getRoundNumber() + 1);
    round.setStatus(SigningRoundStatus.OPEN.name());
    round.setExpiresAt(request.expiresAt());
    round.setCreatedAt(LocalDateTime.now());
    roundMapper.insert(round);

    Set<String> identityHashes = new HashSet<>();
    LocalDateTime invitedAt = LocalDateTime.now();
    for (CreateSigningRoundRequest.SignerRequest signer : request.signers()) {
      String name = requireText(signer.name(), "签署方姓名不能为空");
      String identityNumber = normalizeIdentity(requireText(signer.identityNumber(), "签署方身份证件号码不能为空"));
      String identityHash = identityHasher.sha256(identityNumber);
      if (!identityHashes.add(identityHash)) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "同一签署方不能在一轮邀请中重复出现");
      }

      ContractSignerInvitation invitation = new ContractSignerInvitation();
      invitation.setRoundId(round.getId());
      invitation.setContractId(contractId);
      invitation.setSignerName(name);
      invitation.setSignerEmail(signer.email() == null || signer.email().isBlank() ? null : signer.email().trim());
      invitation.setIdentityHash(identityHash);
      invitation.setIdentityLastFour(lastFour(identityNumber));
      invitation.setInvitationToken(UUID.randomUUID().toString().replace("-", ""));
      invitation.setStatus(SignerInvitationStatus.PENDING.name());
      invitation.setInvitedAt(invitedAt);
      invitationMapper.insert(invitation);
    }

    contract.setStatus(ContractStatus.PENDING_SIGN.name());
    contractMapper.updateById(contract);

    List<ContractSignerInvitation> invitations = listInvitations(round.getId());
    return toCreatedRoundResponse(round, contract.getStatus(), invitations);
  }

  @Transactional
  public SigningResultResponse sign(String token, SignContractRequest request) {
    if (request == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "签署请求不能为空");
    }
    String signerName = requireText(request.signerName(), "签署人姓名不能为空");
    String identityNumber = normalizeIdentity(requireText(request.identityNumber(), "签署人身份证件号码不能为空"));
    String signatureValue = requireText(request.signatureValue(), "本人签名不能为空");

    ContractSignerInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<ContractSignerInvitation>()
        .eq(ContractSignerInvitation::getInvitationToken, token)
        .last("LIMIT 1"));
    if (invitation == null) {
      throw new ApiException(ErrorCode.INVITATION_NOT_FOUND, "签署邀请不存在");
    }

    ContractSigningRound round = roundMapper.selectByIdForUpdate(invitation.getRoundId());
    if (round == null) {
      throw new ApiException(ErrorCode.SIGNING_ROUND_NOT_FOUND, "签署轮次不存在");
    }
    expiryService.expireLockedRoundIfDue(round.getId());
    round = roundMapper.selectByIdForUpdate(invitation.getRoundId());
    List<ContractSignerInvitation> invitations = listInvitations(round.getId());

    if (!invitationSignerMatches(invitation, signerName, identityNumber)) {
      throw new ApiException(ErrorCode.IDENTITY_MISMATCH, "签署人身份与邀请接收人不一致");
    }
    if (SignerInvitationStatus.SIGNED.name().equals(invitation.getStatus())) {
      return toSigningResult(round, invitation, false);
    }
    if (SigningRoundStatus.EXPIRED.name().equals(round.getStatus())
        || SignerInvitationStatus.EXPIRED.name().equals(invitation.getStatus())) {
      throw new ApiException(ErrorCode.INVITATION_EXPIRED, "签署邀请已过期，不能再提交签名");
    }
    if (!SigningRoundStatus.OPEN.name().equals(round.getStatus())) {
      throw new ApiException(ErrorCode.INVALID_SIGNING_STATE, "当前签署轮次已结束");
    }
    if (!round.getExpiresAt().isAfter(LocalDateTime.now())) {
      throw new ApiException(ErrorCode.INVITATION_EXPIRED, "签署邀请已过期，不能再提交签名");
    }

    LocalDateTime signedAt = LocalDateTime.now();
    invitation.setStatus(SignerInvitationStatus.SIGNED.name());
    invitation.setFirstSignedAt(signedAt);
    invitation.setSignatureValue(signatureValue);
    invitationMapper.updateById(invitation);

    long signedCount = invitations.stream()
        .filter(item -> item.getId().equals(invitation.getId()) || SignerInvitationStatus.SIGNED.name().equals(item.getStatus()))
        .count();
    Contract contract = contractMapper.selectById(round.getContractId());
    if (signedCount == invitations.size()) {
      LocalDateTime completedAt = LocalDateTime.now();
      round.setStatus(SigningRoundStatus.COMPLETED.name());
      round.setCompletedAt(completedAt);
      roundMapper.updateById(round);
      contract.setStatus(ContractStatus.SIGNED.name());
      contract.setSignedAt(completedAt);
      contractMapper.updateById(contract);
    }
    return toSigningResult(round, invitation, true);
  }

  @Transactional
  public SigningRoundResponse getProgress(Long contractId) {
    expiryService.expireContractRoundIfDue(contractId);
    Contract contract = contractMapper.selectById(contractId);
    if (contract == null) {
      throw new ApiException(ErrorCode.NOT_FOUND, "合同不存在");
    }
    ContractSigningRound round = roundMapper.selectOne(new LambdaQueryWrapper<ContractSigningRound>()
        .eq(ContractSigningRound::getContractId, contractId)
        .orderByDesc(ContractSigningRound::getRoundNumber)
        .last("LIMIT 1"));
    if (round == null) {
      throw new ApiException(ErrorCode.SIGNING_ROUND_NOT_FOUND, "该合同尚未发起签署邀请");
    }
    List<ContractSignerInvitation> invitations = listInvitations(round.getId());
    return toRoundResponse(round, contract, invitations);
  }

  private List<ContractSignerInvitation> listInvitations(Long roundId) {
    return invitationMapper.selectList(new LambdaQueryWrapper<ContractSignerInvitation>()
        .eq(ContractSignerInvitation::getRoundId, roundId)
        .orderByAsc(ContractSignerInvitation::getId));
  }

  private SigningRoundResponse toRoundResponse(ContractSigningRound round, Contract contract,
      List<ContractSignerInvitation> invitations) {
    List<SignerInvitationResponse> signers = invitations.stream().map(this::toInvitationResponse).toList();
    long signedCount = invitations.stream()
        .filter(item -> SignerInvitationStatus.SIGNED.name().equals(item.getStatus()))
        .count();
    return new SigningRoundResponse(round.getId(), round.getContractId(), round.getRoundNumber(),
        round.getStatus(), contract.getStatus(), round.getExpiresAt(), round.getCompletedAt(),
        round.getCreatedAt(), invitations.size(), (int) signedCount, signers);
  }

  private CreatedSigningRoundResponse toCreatedRoundResponse(ContractSigningRound round, String contractStatus,
      List<ContractSignerInvitation> invitations) {
    List<CreatedSigningRoundResponse.CreatedSignerResponse> signers = invitations.stream()
        .map(item -> new CreatedSigningRoundResponse.CreatedSignerResponse(item.getId(), item.getSignerName(),
            item.getSignerEmail(), item.getIdentityLastFour(), item.getInvitationToken(), item.getStatus(),
            item.getInvitedAt()))
        .toList();
    return new CreatedSigningRoundResponse(round.getId(), round.getContractId(), round.getRoundNumber(),
        round.getStatus(), contractStatus, round.getExpiresAt(), round.getCompletedAt(), round.getCreatedAt(),
        invitations.size(), 0, signers);
  }

  private SignerInvitationResponse toInvitationResponse(ContractSignerInvitation invitation) {
    return new SignerInvitationResponse(invitation.getId(), invitation.getSignerName(),
        invitation.getSignerEmail(), invitation.getIdentityLastFour(), invitation.getStatus(),
        invitation.getInvitedAt(), invitation.getFirstSignedAt(), invitation.getSignatureValue());
  }

  private SigningResultResponse toSigningResult(ContractSigningRound round, ContractSignerInvitation invitation,
      boolean firstSubmission) {
    Contract contract = contractMapper.selectById(round.getContractId());
    return new SigningResultResponse(round.getStatus(), contract.getStatus(), invitation.getSignerName(),
        invitation.getStatus(), invitation.getFirstSignedAt(), invitation.getSignatureValue(), firstSubmission);
  }

  private boolean invitationSignerMatches(ContractSignerInvitation invitation, String signerName,
      String identityNumber) {
    return invitation.getSignerName().equals(signerName)
        && invitation.getIdentityHash().equals(identityHasher.sha256(identityNumber));
  }

  private String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }
    return value.trim();
  }

  private String normalizeIdentity(String identityNumber) {
    return identityNumber.trim().toUpperCase();
  }

  private String lastFour(String identityNumber) {
    return identityNumber.length() <= 4 ? identityNumber : identityNumber.substring(identityNumber.length() - 4);
  }
}
