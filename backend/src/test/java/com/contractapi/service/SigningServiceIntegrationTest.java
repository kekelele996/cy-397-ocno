package com.contractapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.contractapi.constants.ContractStatus;
import com.contractapi.constants.SignerInvitationStatus;
import com.contractapi.constants.SigningRoundStatus;
import com.contractapi.dto.CreateSigningRoundRequest;
import com.contractapi.dto.CreatedSigningRoundResponse;
import com.contractapi.dto.SignContractRequest;
import com.contractapi.dto.SignerInvitationResponse;
import com.contractapi.dto.SigningResultResponse;
import com.contractapi.dto.SigningRoundResponse;
import com.contractapi.entity.Contract;
import com.contractapi.exception.ApiException;
import com.contractapi.mapper.ContractMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SigningServiceIntegrationTest {
  @Autowired private SigningService signingService;
  @Autowired private SigningExpiryService expiryService;
  @Autowired private ContractMapper contractMapper;

  private Long contractId;

  @BeforeEach
  void setUp() {
    Contract contract = new Contract();
    contract.setUserId(1L);
    contract.setTitle("多人签署合同");
    contract.setContent("合同内容");
    contract.setStatus(ContractStatus.DRAFT.name());
    contract.setSigners("[]");
    contractMapper.insert(contract);
    contractId = contract.getId();
  }

  @Test
  void allSignersCompleteBeforeDeadlineAndRepeatedSubmissionKeepsFirstResult() {
    CreatedSigningRoundResponse round = signingService.createRound(contractId, new CreateSigningRoundRequest(
        LocalDateTime.now().plusHours(1),
        List.of(
            new CreateSigningRoundRequest.SignerRequest("张三", "a@example.com", "11010119900101001X"),
            new CreateSigningRoundRequest.SignerRequest("李四", "b@example.com", "110101199002020028")
        )));

    assertEquals(SigningRoundStatus.OPEN.name(), round.status());
    assertEquals(2, round.totalSigners());
    assertEquals(0, round.signedSigners());
    assertEquals(ContractStatus.PENDING_SIGN.name(), round.contractStatus());

    CreatedSigningRoundResponse.CreatedSignerResponse first = round.signers().get(0);
    CreatedSigningRoundResponse.CreatedSignerResponse second = round.signers().get(1);
    SigningResultResponse firstResult = signingService.sign(first.invitationToken(),
        new SignContractRequest("张三", "11010119900101001X", "张三"));
    assertTrue(firstResult.firstSubmission());

    SigningResultResponse repeatedResult = signingService.sign(first.invitationToken(),
        new SignContractRequest("张三", "11010119900101001X", "重复签名"));
    assertFalse(repeatedResult.firstSubmission());
    assertEquals(SigningRoundStatus.OPEN.name(), repeatedResult.roundStatus());

    SigningResultResponse lastResult = signingService.sign(second.invitationToken(),
        new SignContractRequest("李四", "110101199002020028", "李四"));
    assertTrue(lastResult.firstSubmission());
    assertEquals(SigningRoundStatus.COMPLETED.name(), lastResult.roundStatus());
    assertEquals(ContractStatus.SIGNED.name(), lastResult.contractStatus());

    SigningRoundResponse progress = signingService.getProgress(contractId);
    assertEquals(SigningRoundStatus.COMPLETED.name(), progress.status());
    assertEquals(ContractStatus.SIGNED.name(), contractMapper.selectById(contractId).getStatus());
    assertEquals(2, progress.signedSigners());
    assertTrue(progress.signers().stream().allMatch(item -> item.firstSignedAt() != null));
  }

  @Test
  void lateSignatureAfterExpiryDoesNotChangeContractOrSignedRecords() {
    CreatedSigningRoundResponse round = signingService.createRound(contractId, new CreateSigningRoundRequest(
        LocalDateTime.now().plusSeconds(1),
        List.of(
            new CreateSigningRoundRequest.SignerRequest("张三", "a@example.com", "11010119900101001X"),
            new CreateSigningRoundRequest.SignerRequest("李四", "b@example.com", "110101199002020028")
        )));
    signingService.sign(round.signers().get(0).invitationToken(),
        new SignContractRequest("张三", "11010119900101001X", "张三"));

    awaitExpiry();
    expiryService.markDueRounds();

    ApiException ex = assertThrows(ApiException.class, () -> signingService.sign(
        round.signers().get(1).invitationToken(),
        new SignContractRequest("李四", "110101199002020028", "迟到签名")));
    assertEquals("INVITATION_EXPIRED", ex.getCode());

    SigningRoundResponse progress = signingService.getProgress(contractId);
    assertEquals(SigningRoundStatus.EXPIRED.name(), progress.status());
    assertEquals(ContractStatus.EXPIRED.name(), progress.contractStatus());
    assertEquals(SignerInvitationStatus.SIGNED.name(), progress.signers().get(0).status());
    assertEquals(SignerInvitationStatus.EXPIRED.name(), progress.signers().get(1).status());
    assertEquals(1, progress.signedSigners());
    assertEquals(null, contractMapper.selectById(contractId).getSignedAt());
  }

  private void awaitExpiry() {
    try {
      Thread.sleep(1100);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    }
  }
}
