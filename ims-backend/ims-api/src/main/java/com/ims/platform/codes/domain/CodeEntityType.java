package com.ims.platform.codes.domain;

/**
 * Business entities that use sequence-backed codes.
 *
 * <p>Formats (institute id {@code I}, sequence {@code N}):
 *
 * <ul>
 *   <li>STUDENT — {@code I} + 6-digit N → {@code 1000001}
 *   <li>COURSE — {@code C{I}-{N:05}} → {@code C1-00001}
 *   <li>FEE_PLAN — {@code P{I}-{N:05}} → {@code P1-00001}
 *   <li>BATCH — {@code B{I}-{N:05}} → {@code B1-00001}
 *   <li>ACADEMIC_YEAR — {@code AY{I}-{N:04}} → {@code AY1-0001}
 *   <li>FEE_CATEGORY — {@code FC{I}-{N:04}} → {@code FC1-0001}
 *   <li>ADMISSION — {@code A{I}-{N:06}} → {@code A1-000001}
 *   <li>INSTITUTE — {@code I{N:05}} (platform scope) → {@code I00001}
 *   <li>FACULTY — {@code F{I}-{N:05}} → {@code F1-00001}
 *   <li>STAFF — {@code S{I}-{N:05}} → {@code S1-00001}
 * </ul>
 */
public enum CodeEntityType {
  STUDENT,
  COURSE,
  FEE_PLAN,
  BATCH,
  ACADEMIC_YEAR,
  FEE_CATEGORY,
  ADMISSION,
  INSTITUTE,
  FACULTY,
  STAFF
}
