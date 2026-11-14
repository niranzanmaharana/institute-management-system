package com.ims.operations.api;

import java.time.LocalTime;
import java.util.List;

public final class TimetableDtos {

  private TimetableDtos() {}

  public record RoomResponse(Long id, String code, String name, int capacity, String status) {}

  public record TimetableSlotResponse(
      Long id,
      Long batchId,
      Long subjectId,
      String subjectCode,
      String subjectName,
      Long facultyId,
      String facultyCode,
      String facultyName,
      Long roomId,
      String roomCode,
      String roomName,
      int dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      String status) {}

  public record BatchTimetableResponse(Long batchId, List<TimetableSlotResponse> slots) {}
}
