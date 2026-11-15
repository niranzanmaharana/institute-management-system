package com.ims.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ims.notification")
public class NotificationProperties {

  private boolean enabled = true;
  private String channel = "logging";
  private String from = "noreply@ims.local";
  private int maxAttempts = 5;
  private long initialRetrySeconds = 30;
  private double backoffMultiplier = 2.0;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getInitialRetrySeconds() {
    return initialRetrySeconds;
  }

  public void setInitialRetrySeconds(long initialRetrySeconds) {
    this.initialRetrySeconds = initialRetrySeconds;
  }

  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  public void setBackoffMultiplier(double backoffMultiplier) {
    this.backoffMultiplier = backoffMultiplier;
  }
}
