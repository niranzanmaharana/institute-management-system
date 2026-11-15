package com.ims.imports.application;

import com.ims.operations.application.ExamService;
import com.ims.operations.application.ExamService.UpsertResultCommand;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Isolates each marks upsert so a bad row does not poison the import job transaction. */
@Service
public class MarksImportRowWriter {

  private final ExamService examService;

  public MarksImportRowWriter(ExamService examService) {
    this.examService = examService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void upsertOne(Long examId, UpsertResultCommand command) {
    examService.upsertResults(examId, List.of(command));
  }
}
