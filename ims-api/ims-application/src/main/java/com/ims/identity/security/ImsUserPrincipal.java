package com.ims.identity.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class ImsUserPrincipal implements UserDetails {

  private final Long userId;
  private final Long instituteId;
  private final String username;
  private final Collection<? extends GrantedAuthority> authorities;

  public ImsUserPrincipal(
      Long userId,
      Long instituteId,
      String username,
      Collection<? extends GrantedAuthority> authorities) {
    this.userId = userId;
    this.instituteId = instituteId;
    this.username = username;
    this.authorities = authorities;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
