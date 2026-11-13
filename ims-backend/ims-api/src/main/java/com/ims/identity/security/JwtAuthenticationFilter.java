package com.ims.identity.security;

import com.ims.common.tenancy.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        Claims claims = jwtService.parse(token);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        Long instituteId = claims.get("institute_id", Long.class);
        Collection<SimpleGrantedAuthority> authorities = toAuthorities(claims);

        ImsUserPrincipal principal =
            new ImsUserPrincipal(userId, instituteId, username, authorities);
        var authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (instituteId != null) {
          TenantContext.setInstituteId(instituteId);
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }

  @SuppressWarnings("unchecked")
  private Collection<SimpleGrantedAuthority> toAuthorities(Claims claims) {
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    Object roles = claims.get("roles");
    if (roles instanceof Collection<?> roleList) {
      for (Object role : roleList) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      }
    }
    Object permissions = claims.get("permissions");
    if (permissions instanceof Collection<?> permList) {
      for (Object perm : permList) {
        authorities.add(new SimpleGrantedAuthority(String.valueOf(perm)));
      }
    }
    return authorities;
  }
}
