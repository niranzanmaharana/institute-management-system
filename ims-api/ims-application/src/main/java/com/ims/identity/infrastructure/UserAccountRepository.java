package com.ims.identity.infrastructure;

import com.ims.identity.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsernameAndInstituteIdAndDeletedAtIsNull(
      String username, Long instituteId);

  Optional<UserAccount> findByUsernameAndInstituteIdIsNullAndDeletedAtIsNull(String username);

  Optional<UserAccount> findByUsernameAndInstituteId(String username, Long instituteId);

  Optional<UserAccount> findByEmailAndInstituteId(String email, Long instituteId);

  boolean existsByUsernameAndInstituteIdIsNull(String username);

  List<UserAccount> findByInstituteIdAndDeletedAtIsNullOrderByUsernameAsc(Long instituteId);

  Optional<UserAccount> findByIdAndInstituteIdAndDeletedAtIsNull(Long id, Long instituteId);

  @Query(
      """
      SELECT COUNT(u) FROM UserAccount u JOIN u.roles r
      WHERE u.instituteId = :instituteId
        AND u.deletedAt IS NULL
        AND u.status = 'ACTIVE'
        AND r.code = 'ADMIN'
        AND (:excludeUserId IS NULL OR u.id <> :excludeUserId)
      """)
  long countActiveAdmins(
      @Param("instituteId") Long instituteId, @Param("excludeUserId") Long excludeUserId);
}
