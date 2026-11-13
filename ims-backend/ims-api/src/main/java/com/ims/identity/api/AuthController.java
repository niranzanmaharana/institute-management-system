package com.ims.identity.api;

import com.ims.identity.application.AuthService;
import com.ims.identity.application.AuthService.AuthTokens;
import com.ims.identity.application.AuthService.UserView;
import com.ims.identity.security.ImsUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication and current user")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  public record LoginRequest(
      @NotBlank String username,
      @NotBlank String password,
      String instituteCode) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record LogoutRequest(String refreshToken) {}

  @PostMapping("/login")
  @Operation(summary = "Login (omit instituteCode for PLATFORM_ADMIN)")
  public AuthTokens login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    return authService.login(
        request.username(),
        request.password(),
        request.instituteCode(),
        http.getRemoteAddr(),
        http.getHeader("User-Agent"));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh access token")
  public AuthTokens refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request.refreshToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Revoke refresh token")
  @SecurityRequirement(name = "bearer-jwt")
  public void logout(@RequestBody(required = false) LogoutRequest request) {
    authService.logout(request == null ? null : request.refreshToken());
  }

  @GetMapping("/me")
  @Operation(summary = "Current user")
  @SecurityRequirement(name = "bearer-jwt")
  public UserView me(@AuthenticationPrincipal ImsUserPrincipal principal) {
    return authService.me(principal.getUserId());
  }
}
