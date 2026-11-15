package com.ims.notification.application.channel;

import com.ims.notification.domain.NotificationChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local/test sink — records messages in memory and logs at INFO (no SMTP).
 * Enable with {@code ims.notification.channel=logging} (default).
 */
@Component
@ConditionalOnProperty(name = "ims.notification.channel", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailChannelSender implements NotificationChannelSender {

  private static final Logger log = LoggerFactory.getLogger(LoggingEmailChannelSender.class);

  private final List<SentMessage> sent = Collections.synchronizedList(new ArrayList<>());

  public record SentMessage(String from, String to, String subject, String body) {}

  @Override
  public NotificationChannel channel() {
    return NotificationChannel.EMAIL;
  }

  @Override
  public void send(String from, String to, String subject, String body) {
    sent.add(new SentMessage(from, to, subject, body));
    log.info("Notification email sink: to={} subject={}", to, subject);
  }

  public List<SentMessage> snapshot() {
    synchronized (sent) {
      return List.copyOf(sent);
    }
  }

  public void clear() {
    sent.clear();
  }
}
