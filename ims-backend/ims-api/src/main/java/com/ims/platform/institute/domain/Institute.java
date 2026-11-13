package com.ims.platform.institute.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "institutes")
public class Institute {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 32, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(nullable = false, length = 64)
  private String timezone;

  @Column(length = 32)
  private String mobile;

  @Column(name = "admin_email")
  private String adminEmail;

  @Column(length = 512)
  private String website;

  @Column(name = "address_line1")
  private String addressLine1;

  @Column(name = "address_line2")
  private String addressLine2;

  @Column(length = 128)
  private String city;

  @Column(length = 128)
  private String state;

  @Column(name = "postal_code", length = 32)
  private String postalCode;

  @Column(length = 128)
  private String country;

  @Column(name = "icon_url", length = 1024)
  private String iconUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Institute() {}

  public static Institute create(String code, String name, String timezone) {
    Institute institute = new Institute();
    institute.code = code;
    institute.name = name;
    institute.status = "ACTIVE";
    institute.timezone = timezone;
    institute.country = "India";
    institute.createdAt = Instant.now();
    institute.updatedAt = Instant.now();
    return institute;
  }

  public void applyProfile(
      String name,
      String timezone,
      String mobile,
      String adminEmail,
      String website,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      String iconUrl) {
    if (name != null && !name.isBlank()) {
      this.name = name.trim();
    }
    if (timezone != null && !timezone.isBlank()) {
      this.timezone = timezone.trim();
    }
    this.mobile = blankToNull(mobile);
    this.adminEmail = blankToNull(adminEmail);
    this.website = blankToNull(website);
    this.addressLine1 = blankToNull(addressLine1);
    this.addressLine2 = blankToNull(addressLine2);
    this.city = blankToNull(city);
    this.state = blankToNull(state);
    this.postalCode = blankToNull(postalCode);
    this.country = blankToNull(country);
    this.iconUrl = blankToNull(iconUrl);
    this.updatedAt = Instant.now();
  }

  public void suspend() {
    this.status = "SUSPENDED";
    this.updatedAt = Instant.now();
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getStatus() {
    return status;
  }

  public String getTimezone() {
    return timezone;
  }

  public String getMobile() {
    return mobile;
  }

  public String getAdminEmail() {
    return adminEmail;
  }

  public String getWebsite() {
    return website;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountry() {
    return country;
  }

  public String getIconUrl() {
    return iconUrl;
  }
}
