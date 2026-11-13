package com.ims.platform.codes.api;

import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/codes")
@Tag(name = "Codes", description = "Sequence-backed business code generation")
@SecurityRequirement(name = "bearer-jwt")
public class CodeController {

  private final CodeGeneratorService codeGeneratorService;

  public CodeController(CodeGeneratorService codeGeneratorService) {
    this.codeGeneratorService = codeGeneratorService;
  }

  public record NextCodeResponse(String type, String code) {}

  @GetMapping("/next")
  @Operation(
      summary = "Allocate next business code",
      description =
          "Consumes the next sequence value. Leave create-code blank to auto-generate on save, or call this for the Generate button.")
  @PreAuthorize(
      """
      (#type == T(com.ims.platform.codes.domain.CodeEntityType).INSTITUTE and hasAuthority('institute:manage'))
      or (#type != T(com.ims.platform.codes.domain.CodeEntityType).INSTITUTE and isAuthenticated())
      """)
  public NextCodeResponse next(@RequestParam("type") CodeEntityType type) {
    return new NextCodeResponse(type.name(), codeGeneratorService.next(type));
  }
}
