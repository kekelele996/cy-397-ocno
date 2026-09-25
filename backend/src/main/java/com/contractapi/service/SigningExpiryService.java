package com.contractapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.SignerInvitationStatus;
import com.contractapi.constants.SigningRoundStatus;
import com.contractapi.entity.Contract;
import com.contractapi.entity.ContractSignerInvitation;
import com.contractapi.entity.ContractSigningRound;
import com.contractapi.mapper.ContractMapper;
import com.contractapi.mapper.ContractSignerInvitationMapper;
import com.contractapi.mapper.ContractSigningRoundMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SigningExpiryService {
  private final ContractMapper contractMapper;
  private final ContractSigningRoundMapper roundMapper;
  private final ContractSignerInvitationMapper invitationMapper;

  public SigningExpiryService(ContractMapper contractMapper, ContractSigningRoundMapper roundMapper,
      ContractSignerInvitationMapper invitationMapper) {
    this.contractMapper = contractMapper;
    this.roundMapper = roundMapper;
    this.invitationMapper = invitationMapper;
  }

  @Transactional
  public void markDueRounds() {
    roundMapper.selectList(new LambdaQueryWrapper<ContractSigningRound>()
            .eq(ContractSigningRound::getStatus, SigningRoundStatus.OPEN.name())
            .le(ContractSigningRound::getExpiresAt, LocalDateTime.now()))
        .forEach(this::expireRound);
  }

  @Transactional
  public void expireContractRoundIfDue(Long contractId) {
    ContractSigningRound round = roundMapper.selectOne(new LambdaQueryWrapper<ContractSigningRound>()
        .eq(ContractSigningRound::getContractId, contractId)
        .eq(ContractSigningRound::getStatus, SigningRoundStatus.OPEN.name())
        .orderByDesc(ContractSigningRound::getRoundNumber)
        .last("LIMIT 1"));
    expireRoundIfDue(round);
  }

  @Transactional
  public void expireLockedRoundIfDue(Long roundId) {
    ContractSigningRound round = roundMapper.selectByIdForUpdate(roundId);
    expireRoundIfDue(round);
  }

  @Transactional
  public void expireRoundIfDue(ContractSigningRound round) {
    if (round != null && SigningRoundStatus.OPEN.name().equals(round.getStatus())
        && !round.getExpiresAt().isAfter(LocalDateTime.now())) {
      expireRound(round);
    }
  }

  private void expireRound(ContractSigningRound round) {
    int updated = roundMapper.update(null, new LambdaUpdateWrapper<ContractSigningRound>()
        .set(ContractSigningRound::getStatus, SigningRoundStatus.EXPIRED.name())
        .eq(ContractSigningRound::getId, round.getId())
        .eq(ContractSigningRound::getStatus, SigningRoundStatus.OPEN.name()));
    if (updated == 0) {
      return;
    }
    round.setStatus(SigningRoundStatus.EXPIRED.name());

    invitationMapper.update(null, new LambdaUpdateWrapper<ContractSignerInvitation>()
        .set(ContractSignerInvitation::getStatus, SignerInvitationStatus.EXPIRED.name())
        .eq(ContractSignerInvitation::getRoundId, round.getId())
        .eq(ContractSignerInvitation::getStatus, SignerInvitationStatus.PENDING.name()));

    Contract contract = contractMapper.selectById(round.getContractId());
    if (contract != null && ContractStatus.PENDING_SIGN.name().equals(contract.getStatus())) {
      contract.setStatus(ContractStatus.EXPIRED.name());
      contractMapper.updateById(contract);
    }
  }
}
