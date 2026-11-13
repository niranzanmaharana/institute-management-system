package com.ims.people.student.api;

import com.ims.common.paging.PageResponse;
import com.ims.people.student.application.StudentService;
import com.ims.people.student.application.StudentService.AddressInput;
import com.ims.people.student.application.StudentService.CreateStudentCommand;
import com.ims.people.student.application.StudentService.GuardianInput;
import com.ims.people.student.application.StudentService.UpdateStudentCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Tenant-scoped student master data")
@SecurityRequirement(name = "bearer-jwt")
public class StudentController {

  private final StudentService studentService;

  public StudentController(StudentService studentService) {
    this.studentService = studentService;
  }

  public record GuardianRequest(
      @NotBlank @Size(max = 128) String name,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @NotBlank @Size(max = 64) String relation,
      boolean primaryGuardian) {}

  public record AddressRequest(
      @NotBlank @Size(max = 255) String line1,
      @Size(max = 255) String line2,
      @NotBlank @Size(max = 64) String city,
      @Size(max = 64) String state,
      @Size(max = 16) String postalCode,
      @Size(max = 64) String country,
      boolean primaryAddress) {}

  public record CreateStudentRequest(
      @Size(max = 32) String studentCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      List<GuardianRequest> guardians,
      List<AddressRequest> addresses) {}

  public record UpdateStudentRequest(
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      List<GuardianRequest> guardians,
      List<AddressRequest> addresses) {}

  @GetMapping
  @Operation(summary = "List / search students")
  @PreAuthorize("hasAuthority('student:read')")
  public PageResponse<StudentResponse> list(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return studentService.list(q, status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get student by id")
  @PreAuthorize("hasAuthority('student:read')")
  public StudentResponse get(@PathVariable("id") Long id) {
    return studentService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create student")
  @PreAuthorize("hasAuthority('student:write')")
  public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
    return studentService.create(toCreateCommand(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update student (code immutable)")
  @PreAuthorize("hasAuthority('student:write')")
  public StudentResponse update(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateStudentRequest request) {
    return studentService.update(id, toUpdateCommand(request));
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Soft-deactivate student")
  @PreAuthorize("hasAuthority('student:write')")
  public StudentResponse deactivate(@PathVariable("id") Long id) {
    return studentService.deactivate(id);
  }

  private static CreateStudentCommand toCreateCommand(CreateStudentRequest request) {
    return new CreateStudentCommand(
        request.studentCode(),
        request.firstName(),
        request.lastName(),
        request.phone(),
        request.email(),
        mapGuardians(request.guardians()),
        mapAddresses(request.addresses()));
  }

  private static UpdateStudentCommand toUpdateCommand(UpdateStudentRequest request) {
    return new UpdateStudentCommand(
        request.firstName(),
        request.lastName(),
        request.phone(),
        request.email(),
        mapGuardians(request.guardians()),
        mapAddresses(request.addresses()));
  }

  private static List<GuardianInput> mapGuardians(List<GuardianRequest> guardians) {
    if (guardians == null) {
      return null;
    }
    return guardians.stream()
        .map(
            g ->
                new GuardianInput(
                    g.name(), g.phone(), g.email(), g.relation(), g.primaryGuardian()))
        .toList();
  }

  private static List<AddressInput> mapAddresses(List<AddressRequest> addresses) {
    if (addresses == null) {
      return null;
    }
    return addresses.stream()
        .map(
            a ->
                new AddressInput(
                    a.line1(),
                    a.line2(),
                    a.city(),
                    a.state(),
                    a.postalCode(),
                    a.country(),
                    a.primaryAddress()))
        .toList();
  }
}
