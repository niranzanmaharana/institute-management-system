package com.ims.notification.infrastructure;

import com.ims.notification.domain.NotificationDelivery;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryJpaRepository extends JpaRepository<NotificationDelivery, Long> {

  List<NotificationDelivery> findByInstituteIdAndRelatedEntityTypeAndRelatedEntityId(
      Long instituteId, String relatedEntityType, Long relatedEntityId);

  List<NotificationDelivery> findByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAscIdAsc(
      String status, Instant nextRetryAt);
}
