package com.contractapi.controller;

import com.contractapi.dto.CreateInvitationRequest;
import com.contractapi.dto.InvitationProgressResponse;
import com.contractapi.dto.SignRequest;
import com.contractapi.entity.SignInvitation;
import com.contractapi.entity.SignRecord;
import com.contractapi.service.SigningService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contracts/{contractId}")
public class SigningController {
  private final SigningService service;
  public SigningController(SigningService service) { this.service = service; }
  @PostMapping("/invitations") public SignInvitation createInvitation(@PathVariable Long contractId, @RequestBody CreateInvitationRequest request) { return service.createInvitation(contractId, request); }
  @GetMapping("/invitations") public InvitationProgressResponse progress(@PathVariable Long contractId) { return service.progress(contractId); }
  @PostMapping("/sign") public SignRecord sign(@PathVariable Long contractId, @RequestBody SignRequest request) { return service.sign(contractId, request); }
}
