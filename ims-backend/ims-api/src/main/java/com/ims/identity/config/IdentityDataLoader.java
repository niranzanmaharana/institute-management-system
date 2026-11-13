package com.ims.identity.config;

import com.ims.identity.domain.Role;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.RoleRepository;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.platform.institute.infrastructure.InstituteJpaRepository;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdentityDataLoader implements ApplicationRunner {

  public static final String DEFAULT_PASSWORD = "Password@123";

  private static final Logger log = LoggerFactory.getLogger(IdentityDataLoader.class);

  private final UserAccountRepository userAccountRepository;
  private final RoleRepository roleRepository;
  private final InstituteJpaRepository instituteJpaRepository;
  private final PasswordEncoder passwordEncoder;

  public IdentityDataLoader(
      UserAccountRepository userAccountRepository,
      RoleRepository roleRepository,
      InstituteJpaRepository instituteJpaRepository,
      PasswordEncoder passwordEncoder) {
    this.userAccountRepository = userAccountRepository;
    this.roleRepository = roleRepository;
    this.instituteJpaRepository = instituteJpaRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Role platformAdmin = roleRepository.findByCode("PLATFORM_ADMIN").orElseThrow();
    Role admin = roleRepository.findByCode("ADMIN").orElseThrow();
    String hash = passwordEncoder.encode(DEFAULT_PASSWORD);

    if (!userAccountRepository.existsByUsernameAndInstituteIdIsNull("platform")) {
      userAccountRepository.save(
          new UserAccount(
              null,
              "platform",
              "platform@ims.local",
              hash,
              Set.of(platformAdmin)));
      log.info("Seeded PLATFORM_ADMIN user 'platform'");
    }

    instituteJpaRepository
        .findByCode("DEMO_A")
        .ifPresent(
            institute ->
                ensureAdmin(
                    institute.getId(),
                    "admin",
                    "admin.a@demo.local",
                    hash,
                    admin));
    instituteJpaRepository
        .findByCode("DEMO_B")
        .ifPresent(
            institute ->
                ensureAdmin(
                    institute.getId(),
                    "admin",
                    "admin.b@demo.local",
                    hash,
                    admin));
  }

  /**
   * Idempotent seed: skip when username or email already exists for the institute (including
   * soft-deleted rows that still hold the unique key).
   */
  private void ensureAdmin(
      Long instituteId, String username, String email, String hash, Role adminRole) {
    Optional<UserAccount> byUsername =
        userAccountRepository.findByUsernameAndInstituteId(username, instituteId);
    if (byUsername.isPresent()) {
      UserAccount existing = byUsername.get();
      if (!existing.isActive()) {
        existing.activate();
        if (!existing.getRoles().contains(adminRole)) {
          existing.getRoles().add(adminRole);
        }
        userAccountRepository.save(existing);
        log.info("Reactivated ADMIN user '{}' for instituteId={}", username, instituteId);
      }
      return;
    }

    Optional<UserAccount> byEmail =
        userAccountRepository.findByEmailAndInstituteId(email, instituteId);
    if (byEmail.isPresent()) {
      log.info(
          "Skip seeding ADMIN '{}': email {} already used in instituteId={}",
          username,
          email,
          instituteId);
      return;
    }

    userAccountRepository.save(
        new UserAccount(instituteId, username, email, hash, Set.of(adminRole)));
    log.info("Seeded ADMIN user '{}' for instituteId={}", username, instituteId);
  }
}
