package com.ims.notification.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ims.notification.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRetryJob {

  private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

  private final NotificationService notificationService;

  public NotificationRetryJob(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Scheduled(fixedDelayString = "${ims.notification.retry-poll-ms:15000}")
  public void processRetries() {
    int n = notificationService.processDueRetries();
    if (n > 0) {
      log.info("Processed {} due notification deliveries", n);
    }
  }
}
