package com.ims.finance.application;

import com.ims.finance.api.FeeStatementDtos.AdjustmentLine;
import com.ims.finance.api.FeeStatementDtos.FeeStatementResponse;
import com.ims.finance.api.FeeStatementDtos.InvoiceLine;
import com.ims.finance.api.FeeStatementDtos.PaymentLine;
import com.ims.finance.api.FeeStatementDtos.RefundLine;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FeeStatementPdfRenderer {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter DATETIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

  public byte[] render(FeeStatementResponse statement) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Document document = new Document(PageSize.A4, 36, 36, 36, 36);
      PdfWriter.getInstance(document, out);
      document.open();

      Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
      Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);

      document.add(new Paragraph("Fee statement", title));
      document.add(new Paragraph("Generated: " + DATETIME.format(statement.generatedAt()) + " UTC", body));
      document.add(spacer());

      document.add(new Paragraph("Student", heading));
      document.add(
          new Paragraph(
              statement.student().name()
                  + " ("
                  + statement.student().studentCode()
                  + ")"
                  + " · "
                  + nullToDash(statement.student().phone())
                  + " · "
                  + nullToDash(statement.student().email()),
              body));
      document.add(
          new Paragraph(
              "Fee account #"
                  + statement.feeAccount().id()
                  + " · Enrollment #"
                  + statement.feeAccount().enrollmentId()
                  + " · "
                  + statement.feeAccount().currency()
                  + " · "
                  + statement.feeAccount().status(),
              body));
      document.add(spacer());

      document.add(new Paragraph("Totals", heading));
      PdfPTable totals = table(2);
      addRow(totals, "Invoices", money(statement.totals().invoiceTotal()), body);
      addRow(totals, "Payments (SUCCESS)", money(statement.totals().paidTotal()), body);
      addRow(totals, "Credits (discount/scholarship/waiver)", money(statement.totals().creditAdjustments()), body);
      addRow(totals, "Penalties", money(statement.totals().penalties()), body);
      addRow(totals, "Completed refunds", money(statement.totals().completedRefunds()), body);
      addRow(totals, "Outstanding", money(statement.totals().outstandingAmount()), body);
      document.add(totals);
      document.add(spacer());

      document.add(new Paragraph("Invoices", heading));
      PdfPTable invoices = table(6);
      header(invoices, body, "No", "Due", "Amount", "Paid", "Remaining", "Status");
      for (InvoiceLine row : statement.invoices()) {
        invoices.addCell(cell(row.invoiceNo(), body));
        invoices.addCell(cell(DATE.format(row.dueDate()), body));
        invoices.addCell(cell(money(row.amount()), body));
        invoices.addCell(cell(money(row.amountPaid()), body));
        invoices.addCell(cell(money(row.remaining()), body));
        invoices.addCell(cell(row.status(), body));
      }
      document.add(invoices);
      document.add(spacer());

      document.add(new Paragraph("Payments", heading));
      PdfPTable payments = table(6);
      header(payments, body, "Payment", "Paid at", "Method", "Amount", "Status", "Receipt");
      for (PaymentLine row : statement.payments()) {
        payments.addCell(cell(row.paymentNo(), body));
        payments.addCell(cell(DATETIME.format(row.paidAt()), body));
        payments.addCell(cell(row.method(), body));
        payments.addCell(cell(money(row.amount()), body));
        payments.addCell(cell(row.status(), body));
        payments.addCell(cell(nullToDash(row.receiptNo()), body));
      }
      document.add(payments);
      document.add(spacer());

      document.add(new Paragraph("Adjustments", heading));
      PdfPTable adjustments = table(4);
      header(adjustments, body, "When", "Type", "Amount", "Reason");
      for (AdjustmentLine row : statement.adjustments()) {
        adjustments.addCell(cell(DATETIME.format(row.createdAt()), body));
        adjustments.addCell(cell(row.type(), body));
        adjustments.addCell(cell(money(row.amount()), body));
        adjustments.addCell(cell(row.reason(), body));
      }
      document.add(adjustments);
      document.add(spacer());

      document.add(new Paragraph("Refunds", heading));
      PdfPTable refunds = table(4);
      header(refunds, body, "When", "Amount", "Status", "Reason");
      for (RefundLine row : statement.refunds()) {
        refunds.addCell(cell(DATETIME.format(row.createdAt()), body));
        refunds.addCell(cell(money(row.amount()), body));
        refunds.addCell(cell(row.status(), body));
        refunds.addCell(cell(row.reason(), body));
      }
      document.add(refunds);

      document.close();
      return out.toByteArray();
    } catch (DocumentException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF");
    }
  }

  private static Paragraph spacer() {
    Paragraph p = new Paragraph(" ");
    p.setSpacingAfter(6f);
    return p;
  }

  private static PdfPTable table(int columns) {
    PdfPTable table = new PdfPTable(columns);
    table.setWidthPercentage(100);
    table.setSpacingBefore(4f);
    table.setSpacingAfter(4f);
    return table;
  }

  private static void header(PdfPTable table, Font font, String... labels) {
    for (String label : labels) {
      PdfPCell cell = cell(label, font);
      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
      table.addCell(cell);
    }
  }

  private static void addRow(PdfPTable table, String label, String value, Font font) {
    table.addCell(cell(label, font));
    table.addCell(cell(value, font));
  }

  private static PdfPCell cell(String text, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
    cell.setPadding(4f);
    return cell;
  }

  private static String money(BigDecimal amount) {
    return amount == null ? "0.00" : amount.toPlainString();
  }

  private static String nullToDash(String value) {
    return value == null || value.isBlank() ? "—" : value;
  }
}
