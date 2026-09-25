package com.contractapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.SignerInvitationStatus;
import com.contractapi.constants.SigningRoundStatus;
import com.contractapi.dto.SignContractRequest;
import com.contractapi.dto.SigningResultResponse;
import com.contractapi.entity.Contract;
import com.contractapi.entity.ContractSignerInvitation;
import com.contractapi.entity.ContractSigningRound;
import com.contractapi.exception.ApiException;
import com.contractapi.mapper.ContractMapper;
import com.contractapi.mapper.ContractSignerInvitationMapper;
import com.contractapi.mapper.ContractSigningRoundMapper;
import com.contractapi.utils.IdentityHasher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SigningServiceTest {
  @Mock private ContractMapper contractMapper;
  @Mock private ContractSigningRoundMapper roundMapper;
  @Mock private ContractSignerInvitationMapper invitationMapper;
  @Mock private SigningExpiryService expiryService;
  private final IdentityHasher hasher = new IdentityHasher();
  private SigningService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new SigningService(contractMapper, roundMapper, invitationMapper, expiryService, hasher);
  }

  @Test
  void firstSigningRecordsResultAndDoesNotCompleteUntilAllSignersFinish() {
    ContractSigningRound round = round(1L, SigningRoundStatus.OPEN);
    ContractSignerInvitation current = invitation(10L, SignerInvitationStatus.PENDING, hasher.sha256("ID-A"));
    ContractSignerInvitation other = invitation(11L, SignerInvitationStatus.PENDING, hasher.sha256("ID-B"));
    when(invitationMapper.selectOne(any())).thenReturn(current);
    when(roundMapper.selectByIdForUpdate(1L)).thenReturn(round);
    when(invitationMapper.selectList(any())).thenReturn(List.of(current, other));
    Contract contract = new Contract();
    contract.setStatus(ContractStatus.PENDING_SIGN.name());
    when(contractMapper.selectById(100L)).thenReturn(contract);

    SigningResultResponse response = service.sign("token", request("张三", "ID-A", "张三签名"));

    assertTrue(response.firstSubmission());
    assertEquals(SignerInvitationStatus.SIGNED.name(), response.signerStatus());
    assertEquals(SigningRoundStatus.OPEN.name(), response.roundStatus());
    assertEquals(ContractStatus.PENDING_SIGN.name(), response.contractStatus());
    verify(roundMapper, never()).updateById(any(ContractSigningRound.class));
  }

  @Test
  void lastSigningMarksRoundAndContractAsSigned() {
    Contract contract = new Contract();
    contract.setId(100L);
    contract.setStatus(ContractStatus.PENDING_SIGN.name());
    ContractSigningRound round = round(1L, SigningRoundStatus.OPEN);
    ContractSignerInvitation current = invitation(10L, SignerInvitationStatus.PENDING, hasher.sha256("ID-A"));
    ContractSignerInvitation other = invitation(11L, SignerInvitationStatus.SIGNED, hasher.sha256("ID-B"));
    when(invitationMapper.selectOne(any())).thenReturn(current);
    when(roundMapper.selectByIdForUpdate(1L)).thenReturn(round);
    when(invitationMapper.selectList(any())).thenReturn(List.of(current, other));
    when(contractMapper.selectById(100L)).thenReturn(contract);

    SigningResultResponse response = service.sign("token", request("张三", "ID-A", "张三签名"));

    assertEquals(SigningRoundStatus.COMPLETED.name(), response.roundStatus());
    assertEquals(ContractStatus.SIGNED.name(), response.contractStatus());
    assertEquals(SigningRoundStatus.COMPLETED.name(), round.getStatus());
    assertEquals(ContractStatus.SIGNED.name(), contract.getStatus());
  }

  @Test
  void repeatedSubmissionReturnsFirstSignatureWithoutUpdatingRecord() {
    LocalDateTime firstSignedAt = LocalDateTime.now().minusHours(1);
    Contract contract = new Contract();
    contract.setStatus(ContractStatus.SIGNED.name());
    ContractSigningRound round = round(1L, SigningRoundStatus.COMPLETED);
    ContractSignerInvitation invitation = invitation(10L, SignerInvitationStatus.SIGNED, hasher.sha256("ID-A"));
    invitation.setFirstSignedAt(firstSignedAt);
    invitation.setSignatureValue("第一次签名");
    when(invitationMapper.selectOne(any())).thenReturn(invitation);
    when(roundMapper.selectByIdForUpdate(1L)).thenReturn(round);
    when(invitationMapper.selectList(any())).thenReturn(List.of(invitation));
    when(contractMapper.selectById(100L)).thenReturn(contract);

    SigningResultResponse response = service.sign("token", request("张三", "ID-A", "第二次签名"));

    assertFalse(response.firstSubmission());
    assertEquals(firstSignedAt, response.firstSignedAt());
    assertEquals("第一次签名", response.signatureValue());
    assertEquals("第一次签名", invitation.getSignatureValue());
    verify(invitationMapper, never()).updateById(any(ContractSignerInvitation.class));
  }

  @Test
  void signingAfterExpiryIsRejectedAndDoesNotOverwriteRecords() {
    ContractSigningRound round = round(1L, SigningRoundStatus.EXPIRED);
    ContractSignerInvitation invitation = invitation(10L, SignerInvitationStatus.EXPIRED, hasher.sha256("ID-A"));
    when(invitationMapper.selectOne(any())).thenReturn(invitation);
    when(roundMapper.selectByIdForUpdate(1L)).thenReturn(round);
    when(invitationMapper.selectList(any())).thenReturn(List.of(invitation));
    doAnswer(invocation -> {
      round.setStatus(SigningRoundStatus.EXPIRED.name());
      return null;
    }).when(expiryService).expireLockedRoundIfDue(1L);

    ApiException ex = assertThrows(ApiException.class,
        () -> service.sign("token", request("张三", "ID-A", "迟到签名")));

    assertEquals("INVITATION_EXPIRED", ex.getCode());
    assertEquals(SignerInvitationStatus.EXPIRED.name(), invitation.getStatus());
    verify(invitationMapper, never()).updateById(any(ContractSignerInvitation.class));
    verify(roundMapper, never()).updateById(any(ContractSigningRound.class));
  }

  private ContractSigningRound round(Long id, SigningRoundStatus status) {
    ContractSigningRound round = new ContractSigningRound();
    round.setId(id);
    round.setContractId(100L);
    round.setStatus(status.name());
    round.setExpiresAt(LocalDateTime.now().plusHours(1));
    return round;
  }

  private ContractSignerInvitation invitation(Long id, SignerInvitationStatus status, String hash) {
    ContractSignerInvitation invitation = new ContractSignerInvitation();
    invitation.setId(id);
    invitation.setRoundId(1L);
    invitation.setContractId(100L);
    invitation.setSignerName("张三");
    invitation.setIdentityHash(hash);
    invitation.setStatus(status.name());
    return invitation;
  }

  private SignContractRequest request(String name, String identityNumber, String signatureValue) {
    return new SignContractRequest(name, identityNumber, signatureValue);
  }
}
