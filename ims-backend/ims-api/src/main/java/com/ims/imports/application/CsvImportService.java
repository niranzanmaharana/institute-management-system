package com.ims.imports.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.imports.api.ImportDtos.ImportResultResponse;
import com.ims.imports.api.ImportDtos.ImportRowError;
import com.ims.imports.application.CsvSupport.CsvTable;
import com.ims.people.faculty.application.FacultyService;
import com.ims.people.faculty.application.FacultyService.CreateFacultyCommand;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.people.student.application.StudentService.CreateStudentCommand;
import com.ims.people.faculty.api.FacultyResponse;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CsvImportService {

  public static final int MAX_ROWS = 500;
  public static final long MAX_BYTES = 2L * 1024 * 1024;

  public static final String STUDENT_TEMPLATE =
      "studentCode,firstName,lastName,phone,email\n";
  public static final String FACULTY_TEMPLATE =
      "facultyCode,firstName,lastName,phone,email,department\n";

  private final StudentService studentService;
  private final FacultyService facultyService;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public CsvImportService(
      StudentService studentService,
      FacultyService facultyService,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.studentService = studentService;
    this.facultyService = facultyService;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  @Transactional
  public ImportResultResponse importStudents(MultipartFile file) {
    assertCanImport();
    assertHasAuthority("student:write");
    long instituteId = requireTenant();
    CsvTable table = parseFile(file);
    requireHeaders(table.headers(), List.of("firstname", "lastname"));

    List<ImportRowError> errors = new ArrayList<>();
    List<Long> insertedIds = new ArrayList<>();
    int rowNumber = 1; // header is row 1; data starts at 2
    for (Map<String, String> row : table.rows()) {
      rowNumber++;
      String firstName = CsvSupport.cell(row, "firstName", "firstname");
      String lastName = CsvSupport.cell(row, "lastName", "lastname");
      String studentCode = blankToNull(CsvSupport.cell(row, "studentCode", "studentcode", "code"));
      String phone = blankToNull(CsvSupport.cell(row, "phone"));
      String email = blankToNull(CsvSupport.cell(row, "email"));
      String raw = summarize(row);

      if (isBlank(firstName) || isBlank(lastName)) {
        errors.add(new ImportRowError(rowNumber, "firstName/lastName", "firstName and lastName are required", raw));
        continue;
      }
      if (tooLong(firstName, 64) || tooLong(lastName, 64)) {
        errors.add(new ImportRowError(rowNumber, "name", "firstName/lastName max length is 64", raw));
        continue;
      }
      if (tooLong(studentCode, 32) || tooLong(phone, 32) || tooLong(email, 255)) {
        errors.add(new ImportRowError(rowNumber, "fields", "One or more fields exceed max length", raw));
        continue;
      }

      try {
        StudentResponse created =
            studentService.create(
                new CreateStudentCommand(
                    studentCode, firstName, lastName, phone, email, List.of(), List.of()));
        insertedIds.add(created.id());
      } catch (ResponseStatusException ex) {
        errors.add(
            new ImportRowError(
                rowNumber,
                "row",
                ex.getReason() == null ? ex.getMessage() : ex.getReason(),
                raw));
      } catch (RuntimeException ex) {
        errors.add(new ImportRowError(rowNumber, "row", "Could not create student", raw));
      }
    }

    ImportResultResponse result =
        toResult("STUDENT", table.rows().size(), insertedIds, errors);
    // entity_id is NOT NULL — use institute id for bulk summary audits
    auditService.record(
        instituteId,
        "imports",
        "CsvImport",
        instituteId,
        "STUDENTS_CSV_IMPORTED",
        null,
        Map.of(
            "totalRows", result.totalRows(),
            "inserted", result.inserted(),
            "failed", result.failed(),
            "insertedIds", insertedIds),
        null);
    return result;
  }

  @Transactional
  public ImportResultResponse importFaculties(MultipartFile file) {
    assertCanImport();
    assertHasAuthority("faculty:write");
    long instituteId = requireTenant();
    CsvTable table = parseFile(file);
    requireHeaders(table.headers(), List.of("firstname", "lastname"));

    List<ImportRowError> errors = new ArrayList<>();
    List<Long> insertedIds = new ArrayList<>();
    int rowNumber = 1;
    for (Map<String, String> row : table.rows()) {
      rowNumber++;
      String firstName = CsvSupport.cell(row, "firstName", "firstname");
      String lastName = CsvSupport.cell(row, "lastName", "lastname");
      String facultyCode = blankToNull(CsvSupport.cell(row, "facultyCode", "facultycode", "code"));
      String phone = blankToNull(CsvSupport.cell(row, "phone"));
      String email = blankToNull(CsvSupport.cell(row, "email"));
      String department = blankToNull(CsvSupport.cell(row, "department"));
      String raw = summarize(row);

      if (isBlank(firstName) || isBlank(lastName)) {
        errors.add(new ImportRowError(rowNumber, "firstName/lastName", "firstName and lastName are required", raw));
        continue;
      }
      if (tooLong(firstName, 64)
          || tooLong(lastName, 64)
          || tooLong(facultyCode, 32)
          || tooLong(phone, 32)
          || tooLong(email, 255)
          || tooLong(department, 128)) {
        errors.add(new ImportRowError(rowNumber, "fields", "One or more fields exceed max length", raw));
        continue;
      }

      try {
        FacultyResponse created =
            facultyService.create(
                new CreateFacultyCommand(
                    facultyCode, firstName, lastName, phone, email, department, List.of()));
        insertedIds.add(created.id());
      } catch (ResponseStatusException ex) {
        errors.add(
            new ImportRowError(
                rowNumber,
                "row",
                ex.getReason() == null ? ex.getMessage() : ex.getReason(),
                raw));
      } catch (RuntimeException ex) {
        errors.add(new ImportRowError(rowNumber, "row", "Could not create faculty", raw));
      }
    }

    ImportResultResponse result =
        toResult("FACULTY", table.rows().size(), insertedIds, errors);
    auditService.record(
        instituteId,
        "imports",
        "CsvImport",
        instituteId,
        "FACULTIES_CSV_IMPORTED",
        null,
        Map.of(
            "totalRows", result.totalRows(),
            "inserted", result.inserted(),
            "failed", result.failed(),
            "insertedIds", insertedIds),
        null);
    return result;
  }

  private CsvTable parseFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is required");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file exceeds 2 MB limit");
    }
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    if (!name.endsWith(".csv")
        && file.getContentType() != null
        && !file.getContentType().contains("csv")
        && !file.getContentType().contains("text")) {
      // allow missing extension if content looks like text/csv
    }
    try (InputStream in = file.getInputStream()) {
      return CsvSupport.parse(in, MAX_ROWS);
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file", ex);
    }
  }

  private static void requireHeaders(List<String> headers, List<String> required) {
    for (String req : required) {
      if (headers.stream().noneMatch(h -> h.equals(req))) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "CSV must include column: " + req);
      }
    }
  }

  private ImportResultResponse toResult(
      String entityType, int totalRows, List<Long> insertedIds, List<ImportRowError> errors) {
    List<List<String>> errorRows = new ArrayList<>();
    for (ImportRowError error : errors) {
      errorRows.add(
          List.of(
              String.valueOf(error.rowNumber()),
              nullToEmpty(error.field()),
              nullToEmpty(error.message()),
              nullToEmpty(error.raw())));
    }
    String report =
        errors.isEmpty()
            ? ""
            : CsvSupport.toCsv(List.of("rowNumber", "field", "message", "raw"), errorRows);
    return new ImportResultResponse(
        entityType,
        totalRows,
        insertedIds.size(),
        errors.size(),
        List.copyOf(insertedIds),
        List.copyOf(errors),
        report);
  }

  private void assertCanImport() {
    if (!hasAuthority("import:write")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "import:write required");
    }
  }

  private void assertHasAuthority(String code) {
    if (!hasAuthority(code)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, code + " required");
    }
  }

  private boolean hasAuthority(String code) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(code::equals);
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private static boolean tooLong(String value, int max) {
    return value != null && value.length() > max;
  }

  private static String summarize(Map<String, String> row) {
    return String.join(
        "|",
        row.entrySet().stream()
            .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
            .toList());
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
