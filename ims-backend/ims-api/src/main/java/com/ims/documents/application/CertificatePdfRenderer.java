package com.ims.documents.application;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CertificatePdfRenderer {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter ISSUED =
      DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC);

  public record Letterhead(
      String instituteName,
      String addressLine,
      String cityState,
      String contactLine) {}

  public record StudentLine(String fullName, String studentCode) {}

  public record CourseLine(String courseName, String batchName, LocalDate startDate, LocalDate endDate) {}

  public byte[] renderBonafide(Letterhead letterhead, StudentLine student, CourseLine course) {
    return render(
        "BONAFIDE CERTIFICATE",
        letterhead,
        student,
        """
        This is to certify that %s (Student ID: %s) is a bona fide student of this institute,
        enrolled in %s — batch %s%s.
        """
            .formatted(
                student.fullName(),
                student.studentCode(),
                course.courseName(),
                course.batchName(),
                dateRangeSuffix(course)),
        "This certificate is issued for official purposes.");
  }

  public byte[] renderCompletion(Letterhead letterhead, StudentLine student, CourseLine course) {
    return render(
        "COURSE COMPLETION CERTIFICATE",
        letterhead,
        student,
        """
        This is to certify that %s (Student ID: %s) has successfully completed the course
        %s (batch %s)%s.
        """
            .formatted(
                student.fullName(),
                student.studentCode(),
                course.courseName(),
                course.batchName(),
                dateRangeSuffix(course)),
        "We congratulate the student on successful completion.");
  }

  private byte[] render(
      String title,
      Letterhead letterhead,
      StudentLine student,
      String bodyText,
      String closing) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Document document = new Document(PageSize.A4, 48, 48, 48, 48);
      PdfWriter.getInstance(document, out);
      document.open();

      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
      Font body = FontFactory.getFont(FontFactory.HELVETICA, 11);
      Font muted = FontFactory.getFont(FontFactory.HELVETICA, 9);

      document.add(new Paragraph(nullToDash(letterhead.instituteName()), titleFont));
      if (notBlank(letterhead.addressLine())) {
        document.add(new Paragraph(letterhead.addressLine(), muted));
      }
      if (notBlank(letterhead.cityState())) {
        document.add(new Paragraph(letterhead.cityState(), muted));
      }
      if (notBlank(letterhead.contactLine())) {
        document.add(new Paragraph(letterhead.contactLine(), muted));
      }
      document.add(spacer());
      document.add(new Paragraph(title, heading));
      document.add(spacer());
      document.add(new Paragraph(bodyText.replace('\n', ' ').trim(), body));
      document.add(spacer());
      document.add(new Paragraph(closing, body));
      document.add(spacer());
      document.add(spacer());
      document.add(
          new Paragraph(
              "Issued on " + ISSUED.format(Instant.now()) + " for " + student.fullName() + ".",
              muted));
      document.add(new Paragraph("Authorized signatory", muted));

      document.close();
      return out.toByteArray();
    } catch (DocumentException ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not render certificate PDF", ex);
    }
  }

  private static String dateRangeSuffix(CourseLine course) {
    if (course.startDate() == null && course.endDate() == null) {
      return "";
    }
    String start = course.startDate() == null ? "—" : DATE.format(course.startDate());
    String end = course.endDate() == null ? "—" : DATE.format(course.endDate());
    return " (period " + start + " to " + end + ")";
  }

  private static Paragraph spacer() {
    return new Paragraph(" ");
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static String nullToDash(String value) {
    return notBlank(value) ? value.trim() : "—";
  }
}
