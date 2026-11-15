package com.ims.notification.application.channel;

import com.ims.notification.domain.NotificationChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ims.notification.channel", havingValue = "smtp")
public class SmtpEmailChannelSender implements NotificationChannelSender {

  private final JavaMailSender mailSender;

  public SmtpEmailChannelSender(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public NotificationChannel channel() {
    return NotificationChannel.EMAIL;
  }

  @Override
  public void send(String from, String to, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    mailSender.send(message);
  }
}
