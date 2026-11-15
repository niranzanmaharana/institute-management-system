package com.ims.notification.infrastructure;

import com.ims.notification.domain.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplate, Long> {

  Optional<NotificationTemplate> findByCodeAndChannelAndActiveTrue(String code, String channel);
}
