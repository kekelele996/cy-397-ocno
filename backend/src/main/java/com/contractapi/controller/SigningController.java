package com.contractapi.controller;

import com.contractapi.dto.CreateSigningRoundRequest;
import com.contractapi.dto.CreatedSigningRoundResponse;
import com.contractapi.dto.SignContractRequest;
import com.contractapi.dto.SigningResultResponse;
import com.contractapi.dto.SigningRoundResponse;
import com.contractapi.service.SigningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class SigningController {
  private final SigningService service;

  public SigningController(SigningService service) {
    this.service = service;
  }

  @PostMapping("/{contractId}/signing-rounds")
  public CreatedSigningRoundResponse createRound(@PathVariable Long contractId,
      @RequestBody CreateSigningRoundRequest request) {
    return service.createRound(contractId, request);
  }

  @GetMapping("/{contractId}/signing-progress")
  public SigningRoundResponse getProgress(@PathVariable Long contractId) {
    return service.getProgress(contractId);
  }

  @PostMapping("/signing-invitations/{token}/sign")
  public SigningResultResponse sign(@PathVariable String token, @RequestBody SignContractRequest request) {
    return service.sign(token, request);
  }
}
