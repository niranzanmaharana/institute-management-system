package com.ims.platform.institute.api;

import com.ims.platform.institute.application.InstituteBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/institutes")
@Tag(name = "Public institute branding", description = "Login-page branding by institute code")
public class PublicInstituteBrandingController {

  private final InstituteBrandingService instituteBrandingService;

  public PublicInstituteBrandingController(InstituteBrandingService instituteBrandingService) {
    this.instituteBrandingService = instituteBrandingService;
  }

  @GetMapping("/{code}/branding")
  @Operation(summary = "Public branding for an active institute code")
  public InstituteBrandingResponse branding(@PathVariable("code") String code) {
    return instituteBrandingService.brandingByCode(code);
  }
}
