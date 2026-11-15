package com.ims.platform.institute.application;

import com.ims.identity.security.ImsUserPrincipal;
import com.ims.platform.institute.api.InstituteBrandingResponse;
import com.ims.platform.institute.api.InstituteLogoUploadResponse;
import com.ims.platform.institute.api.InstituteResponse;
import com.ims.platform.institute.domain.Institute;
import com.ims.platform.institute.infrastructure.InstituteJpaRepository;
import com.ims.platform.storage.ObjectStorage;
import com.ims.platform.storage.StorageProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InstituteBrandingService {

  private static final Set<String> LOGO_TYPES = Set.of("image/jpeg", "image/png");
  private static final long MAX_LOGO_BYTES = 5L * 1024 * 1024;

  private final InstituteJpaRepository instituteJpaRepository;
  private final ObjectStorage objectStorage;
  private final StorageProperties storageProperties;

  public InstituteBrandingService(
      InstituteJpaRepository instituteJpaRepository,
      ObjectStorage objectStorage,
      StorageProperties storageProperties) {
    this.instituteJpaRepository = instituteJpaRepository;
    this.objectStorage = objectStorage;
    this.storageProperties = storageProperties;
  }

  @Transactional(readOnly = true)
  public InstituteBrandingResponse brandingByCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institute code is required");
    }
    Institute institute =
        instituteJpaRepository
            .findByCode(code.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found"));
    if (!"ACTIVE".equals(institute.getStatus())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found");
    }
    return toBranding(institute);
  }

  @Transactional(readOnly = true)
  public InstituteBrandingResponse brandingCurrent(ImsUserPrincipal principal) {
    if (principal == null || principal.getInstituteId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No institute branding for this user");
    }
    Institute institute =
        instituteJpaRepository
            .findById(principal.getInstituteId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found"));
    return toBranding(institute);
  }

  @Transactional
  public InstituteLogoUploadResponse initLogoUpload(
      Long instituteId,
      String fileName,
      String contentType,
      long fileSize,
      ImsUserPrincipal principal) {
    Institute institute = requireWritableInstitute(instituteId, principal);
    String normalizedType = normalizeLogoType(contentType);
    validateSize(fileSize);
    String safeName = sanitizeFileName(fileName);
    validateExtension(safeName, normalizedType);

    String storageKey =
        "institutes/"
            + institute.getId()
            + "/logo/"
            + UUID.randomUUID()
            + "-"
            + safeName;
    Duration expiry = Duration.ofMinutes(Math.max(1, storageProperties.presignExpiryMinutes()));
    String uploadUrl = objectStorage.presignPut(storageKey, normalizedType, fileSize, expiry);
    return new InstituteLogoUploadResponse(
        storageKey, uploadUrl, Instant.now().plus(expiry), Map.of("Content-Type", normalizedType));
  }

  @Transactional
  public InstituteResponse completeLogoUpload(
      Long instituteId, String storageKey, ImsUserPrincipal principal) {
    Institute institute = requireWritableInstitute(instituteId, principal);
    if (storageKey == null || storageKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage key is required");
    }
    String expectedPrefix = "institutes/" + institute.getId() + "/logo/";
    if (!storageKey.startsWith(expectedPrefix)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage key");
    }
    if (objectStorage.head(storageKey).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo object not found in storage");
    }
    institute.setIconUrl(storageKey);
    Institute saved = instituteJpaRepository.save(institute);
    return toInstituteResponse(saved);
  }

  public String resolveDisplayUrl(String iconUrl) {
    if (iconUrl == null || iconUrl.isBlank()) {
      return null;
    }
    String trimmed = iconUrl.trim();
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      return trimmed;
    }
    Duration expiry = Duration.ofMinutes(Math.max(1, storageProperties.presignExpiryMinutes()));
    String contentType = contentTypeFromKey(trimmed);
    String fileName = trimmed.substring(trimmed.lastIndexOf('/') + 1);
    return objectStorage.presignGet(trimmed, fileName, contentType, expiry, false);
  }

  private Institute requireWritableInstitute(Long instituteId, ImsUserPrincipal principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    Institute institute =
        instituteJpaRepository
            .findById(instituteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found"));
    boolean platform =
        principal.getAuthorities().stream()
            .anyMatch(a -> "ROLE_PLATFORM_ADMIN".equals(a.getAuthority()));
    if (platform) {
      return institute;
    }
    if (principal.getInstituteId() == null || !principal.getInstituteId().equals(instituteId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Institute not found");
    }
    boolean admin =
        principal.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    if (!admin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only institute admins can update the logo");
    }
    return institute;
  }

  private InstituteBrandingResponse toBranding(Institute institute) {
    return new InstituteBrandingResponse(
        institute.getId(),
        institute.getCode(),
        institute.getName(),
        institute.getIconUrl(),
        resolveDisplayUrl(institute.getIconUrl()));
  }

  private InstituteResponse toInstituteResponse(Institute institute) {
    return new InstituteResponse(
        institute.getId(),
        institute.getCode(),
        institute.getName(),
        institute.getStatus(),
        institute.getTimezone(),
        institute.getMobile(),
        institute.getAdminEmail(),
        institute.getWebsite(),
        institute.getAddressLine1(),
        institute.getAddressLine2(),
        institute.getCity(),
        institute.getState(),
        institute.getPostalCode(),
        institute.getCountry(),
        resolveDisplayUrl(institute.getIconUrl()));
  }

  private static String normalizeLogoType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content type is required");
    }
    String normalized = contentType.trim().toLowerCase(Locale.ROOT);
    int semicolon = normalized.indexOf(';');
    if (semicolon >= 0) {
      normalized = normalized.substring(0, semicolon).trim();
    }
    if ("image/jpg".equals(normalized)) {
      normalized = "image/jpeg";
    }
    if (!LOGO_TYPES.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo must be JPEG or PNG");
    }
    return normalized;
  }

  private static void validateSize(long fileSize) {
    if (fileSize <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must be greater than 0");
    }
    if (fileSize > MAX_LOGO_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo exceeds the maximum size of 5 MB");
    }
  }

  private static String sanitizeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is required");
    }
    String base = fileName.replace('\\', '/');
    int slash = base.lastIndexOf('/');
    if (slash >= 0) {
      base = base.substring(slash + 1);
    }
    String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
    if (cleaned.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is invalid");
    }
    if (cleaned.length() > 120) {
      cleaned = cleaned.substring(cleaned.length() - 120);
    }
    return cleaned;
  }

  private static void validateExtension(String fileName, String contentType) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    boolean jpeg = lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    boolean png = lower.endsWith(".png");
    if ("image/jpeg".equals(contentType) && !jpeg) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPEG logo must use .jpg or .jpeg");
    }
    if ("image/png".equals(contentType) && !png) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PNG logo must use .png");
    }
  }

  private static String contentTypeFromKey(String storageKey) {
    String lower = storageKey.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    return "image/jpeg";
  }
}
