package com.ims.platform.codes.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.codes.domain.CodeSequence;
import com.ims.platform.codes.infrastructure.CodeSequenceJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CodeGeneratorService {

  /** Platform-scoped counters (institute codes). */
  public static final long PLATFORM_SCOPE_ID = 0L;

  private final CodeSequenceJpaRepository codeSequenceJpaRepository;

  public CodeGeneratorService(CodeSequenceJpaRepository codeSequenceJpaRepository) {
    this.codeSequenceJpaRepository = codeSequenceJpaRepository;
  }

  /**
   * Allocates the next code for the type. Consumes a sequence number (gaps OK if form abandoned).
   */
  @Transactional
  public String next(CodeEntityType type) {
    long scopeId = scopeFor(type);
    long seq = allocate(scopeId, type);
    return format(type, scopeId, seq);
  }

  /** Uses a provided code when present; otherwise allocates the next sequence value. */
  @Transactional
  public String resolve(CodeEntityType type, String provided) {
    if (provided != null && !provided.isBlank()) {
      return provided.trim().toUpperCase();
    }
    return next(type);
  }

  private long scopeFor(CodeEntityType type) {
    if (type == CodeEntityType.INSTITUTE) {
      return PLATFORM_SCOPE_ID;
    }
    Long instituteId = TenantContext.getInstituteId();
    if (instituteId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Institute context required to generate " + type + " code");
    }
    return instituteId;
  }

  private long allocate(long instituteId, CodeEntityType type) {
    CodeSequence sequence =
        codeSequenceJpaRepository.findForUpdate(instituteId, type).orElse(null);
    if (sequence == null) {
      try {
        codeSequenceJpaRepository.saveAndFlush(CodeSequence.start(instituteId, type, 1L));
      } catch (DataIntegrityViolationException ignored) {
        // Concurrent first insert — fall through to locked read.
      }
      sequence =
          codeSequenceJpaRepository
              .findForUpdate(instituteId, type)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Failed to create code sequence for " + type + " @ " + instituteId));
    }
    long allocated = sequence.allocateNext();
    codeSequenceJpaRepository.save(sequence);
    return allocated;
  }

  static String format(CodeEntityType type, long instituteId, long seq) {
    return switch (type) {
      case STUDENT -> instituteId + String.format("%06d", seq);
      case COURSE -> "C" + instituteId + "-" + String.format("%05d", seq);
      case FEE_PLAN -> "P" + instituteId + "-" + String.format("%05d", seq);
      case BATCH -> "B" + instituteId + "-" + String.format("%05d", seq);
      case ACADEMIC_YEAR -> "AY" + instituteId + "-" + String.format("%04d", seq);
      case FEE_CATEGORY -> "FC" + instituteId + "-" + String.format("%04d", seq);
      case ADMISSION -> "A" + instituteId + "-" + String.format("%06d", seq);
      case INSTITUTE -> "I" + String.format("%05d", seq);
      case FACULTY -> "F" + instituteId + "-" + String.format("%05d", seq);
      case STAFF -> "S" + instituteId + "-" + String.format("%05d", seq);
    };
  }
}
