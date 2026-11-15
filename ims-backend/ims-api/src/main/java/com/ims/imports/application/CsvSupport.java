package com.ims.imports.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared CSV parse/escape helpers for people import and bulk marks. */
public final class CsvSupport {

  private CsvSupport() {}

  public record CsvTable(List<String> headers, List<Map<String, String>> rows) {}

  public static CsvTable parse(InputStream in, int maxRows) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null || headerLine.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV header row is required");
      }
      if (headerLine.startsWith("\uFEFF")) {
        headerLine = headerLine.substring(1);
      }
      List<String> headers = parseLine(headerLine).stream().map(CsvSupport::normalizeHeader).toList();
      if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV header row is empty");
      }
      List<Map<String, String>> rows = new ArrayList<>();
      String line;
      int dataRows = 0;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        dataRows++;
        if (dataRows > maxRows) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "CSV exceeds maximum of " + maxRows + " data rows");
        }
        List<String> values = parseLine(line);
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
          String key = headers.get(i);
          if (key.isBlank()) {
            continue;
          }
          String value = i < values.size() ? values.get(i) : "";
          row.put(key, value == null ? "" : value.trim());
        }
        rows.add(row);
      }
      return new CsvTable(headers, rows);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file", ex);
    }
  }

  public static String toCsv(List<String> headers, List<List<String>> rows) {
    StringWriter writer = new StringWriter();
    writer.write(joinLine(headers));
    writer.write('\n');
    for (List<String> row : rows) {
      writer.write(joinLine(row));
      writer.write('\n');
    }
    return writer.toString();
  }

  public static String cell(Map<String, String> row, String... keys) {
    for (String key : keys) {
      String normalized = normalizeHeader(key);
      if (row.containsKey(normalized)) {
        String value = row.get(normalized);
        return value == null ? "" : value.trim();
      }
    }
    return "";
  }

  public static String normalizeHeader(String header) {
    if (header == null) {
      return "";
    }
    return header.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
  }

  private static List<String> parseLine(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else if (c == '"') {
        inQuotes = true;
      } else if (c == ',') {
        cells.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    cells.add(current.toString());
    return cells;
  }

  private static String joinLine(List<String> cells) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escape(cells.get(i)));
    }
    return sb.toString();
  }

  private static String escape(String value) {
    String v = value == null ? "" : value;
    if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
      return "\"" + v.replace("\"", "\"\"") + "\"";
    }
    return v;
  }
}
