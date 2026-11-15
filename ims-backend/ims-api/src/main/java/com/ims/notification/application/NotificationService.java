package com.ims.notification.application;

import com.ims.notification.application.channel.NotificationChannelSender;
import com.ims.notification.domain.NotificationChannel;
import com.ims.notification.domain.NotificationDelivery;
import com.ims.notification.domain.NotificationDeliveryStatus;
import com.ims.notification.domain.NotificationTemplate;
import com.ims.notification.infrastructure.NotificationDeliveryJpaRepository;
import com.ims.notification.infrastructure.NotificationTemplateJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationTemplateJpaRepository templateRepository;
  private final NotificationDeliveryJpaRepository deliveryRepository;
  private final TemplateRenderer templateRenderer;
  private final NotificationChannelSender channelSender;
  private final NotificationProperties properties;

  public NotificationService(
      NotificationTemplateJpaRepository templateRepository,
      NotificationDeliveryJpaRepository deliveryRepository,
      TemplateRenderer templateRenderer,
      NotificationChannelSender channelSender,
      NotificationProperties properties) {
    this.templateRepository = templateRepository;
    this.deliveryRepository = deliveryRepository;
    this.templateRenderer = templateRenderer;
    this.channelSender = channelSender;
    this.properties = properties;
  }

  /** Own transaction so delivery logging survives the caller's commit/rollback boundary. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<NotificationDelivery> enqueue(
      Long instituteId,
      String triggerCode,
      String templateCode,
      String recipient,
      Map<String, String> vars,
      String relatedEntityType,
      Long relatedEntityId) {
    if (!properties.isEnabled()) {
      log.debug("Notifications disabled; skip trigger={}", triggerCode);
      return Optional.empty();
    }

    String channel = NotificationChannel.EMAIL.name();
    if (recipient == null || recipient.isBlank() || !recipient.contains("@")) {
      NotificationDelivery skipped =
          NotificationDelivery.create(
              instituteId,
              templateCode,
              channel,
              triggerCode,
              recipient == null || recipient.isBlank() ? "(none)" : recipient.trim(),
              "(skipped)",
              "(no recipient email)",
              relatedEntityType,
              relatedEntityId,
              properties.getMaxAttempts());
      skipped.markSkipped("Missing or invalid recipient email");
      return Optional.of(deliveryRepository.saveAndFlush(skipped));
    }

    NotificationTemplate template =
        templateRepository
            .findByCodeAndChannelAndActiveTrue(templateCode, channel)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Active notification template not found: " + templateCode));

    String subject = templateRenderer.render(template.getSubjectTemplate(), vars);
    String body = templateRenderer.render(template.getBodyTemplate(), vars);
    NotificationDelivery delivery =
        NotificationDelivery.create(
            instituteId,
            templateCode,
            channel,
            triggerCode,
            recipient.trim(),
            subject,
            body,
            relatedEntityType,
            relatedEntityId,
            properties.getMaxAttempts());
    delivery = deliveryRepository.saveAndFlush(delivery);
    attemptSend(delivery);
    return Optional.of(deliveryRepository.saveAndFlush(delivery));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int processDueRetries() {
    if (!properties.isEnabled()) {
      return 0;
    }
    List<NotificationDelivery> due =
        deliveryRepository.findByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAscIdAsc(
            NotificationDeliveryStatus.PENDING, Instant.now());
    int processed = 0;
    for (NotificationDelivery delivery : due) {
      if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
        delivery.markAttemptFailed("Max attempts already reached", null, true);
        deliveryRepository.saveAndFlush(delivery);
        continue;
      }
      attemptSend(delivery);
      deliveryRepository.saveAndFlush(delivery);
      processed++;
    }
    return processed;
  }

  @Transactional(readOnly = true)
  public List<NotificationDelivery> listForEntity(
      Long instituteId, String relatedEntityType, Long relatedEntityId) {
    return deliveryRepository.findByInstituteIdAndRelatedEntityTypeAndRelatedEntityId(
        instituteId, relatedEntityType, relatedEntityId);
  }

  private void attemptSend(NotificationDelivery delivery) {
    try {
      if (!NotificationChannel.EMAIL.name().equals(delivery.getChannel())) {
        throw new IllegalStateException("Unsupported channel: " + delivery.getChannel());
      }
      if (channelSender.channel() != NotificationChannel.EMAIL) {
        throw new IllegalStateException("No EMAIL channel sender configured");
      }
      channelSender.send(
          properties.getFrom(), delivery.getRecipient(), delivery.getSubject(), delivery.getBody());
      delivery.markSent();
      log.info(
          "Notification SENT id={} trigger={} to={} instituteId={}",
          delivery.getId(),
          delivery.getTriggerCode(),
          delivery.getRecipient(),
          delivery.getInstituteId());
    } catch (Exception ex) {
      Instant next = nextRetryAt(delivery.getAttemptCount() + 1);
      boolean exhausted = delivery.getAttemptCount() + 1 >= delivery.getMaxAttempts();
      delivery.markAttemptFailed(ex.getMessage(), next, exhausted);
      log.warn(
          "Notification send failed id={} attempt={}/{} exhausted={} err={}",
          delivery.getId(),
          delivery.getAttemptCount(),
          delivery.getMaxAttempts(),
          exhausted,
          ex.getMessage());
    }
  }

  private Instant nextRetryAt(int attemptNumber) {
    double factor = Math.pow(properties.getBackoffMultiplier(), Math.max(0, attemptNumber - 1));
    long delaySeconds = Math.round(properties.getInitialRetrySeconds() * factor);
    return Instant.now().plusSeconds(Math.max(1, delaySeconds));
  }
}
