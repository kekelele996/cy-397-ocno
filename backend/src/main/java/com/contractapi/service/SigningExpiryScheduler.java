package com.contractapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SigningExpiryScheduler {
  private static final Logger log = LoggerFactory.getLogger(SigningExpiryScheduler.class);
  private final SigningExpiryService expiryService;

  public SigningExpiryScheduler(SigningExpiryService expiryService) {
    this.expiryService = expiryService;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 1000)
  public void markDueRounds() {
    try {
      expiryService.markDueRounds();
    } catch (RuntimeException ex) {
      log.warn("Failed to mark signing rounds as expired", ex);
    }
  }
}
