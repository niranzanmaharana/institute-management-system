package com.ims.imports.application;

import com.ims.common.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MarksImportAsyncRunner {

  private static final Logger log = LoggerFactory.getLogger(MarksImportAsyncRunner.class);

  private final MarksBulkImportService marksBulkImportService;

  public MarksImportAsyncRunner(@Lazy MarksBulkImportService marksBulkImportService) {
    this.marksBulkImportService = marksBulkImportService;
  }

  @Async
  public void processAsync(Long jobId, Long instituteId, SecurityContext securityContext) {
    try {
      SecurityContextHolder.setContext(securityContext);
      TenantContext.setInstituteId(instituteId);
      marksBulkImportService.processJob(jobId);
    } catch (Exception ex) {
      log.warn("Async marks import jobId={} failed: {}", jobId, ex.getMessage());
    } finally {
      SecurityContextHolder.clearContext();
      TenantContext.clear();
    }
  }
}
