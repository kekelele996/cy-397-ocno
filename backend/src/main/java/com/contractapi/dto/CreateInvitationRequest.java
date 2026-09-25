package com.contractapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreateInvitationRequest(List<String> signers, LocalDateTime deadline) {}
