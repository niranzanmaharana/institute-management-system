package com.ims.notification.application.channel;

import com.ims.notification.domain.NotificationChannel;

public interface NotificationChannelSender {

  NotificationChannel channel();

  void send(String from, String to, String subject, String body) throws Exception;
}
