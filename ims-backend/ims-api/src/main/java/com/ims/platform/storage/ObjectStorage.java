package com.ims.platform.storage;

import java.time.Duration;
import java.util.Optional;

public interface ObjectStorage {

  String presignPut(String storageKey, String contentType, long contentLength, Duration expiry);

  default String presignGet(
      String storageKey, String fileName, String contentType, Duration expiry) {
    return presignGet(storageKey, fileName, contentType, expiry, true);
  }

  String presignGet(
      String storageKey, String fileName, String contentType, Duration expiry, boolean attachment);

  Optional<StoredObjectMeta> head(String storageKey);

  record StoredObjectMeta(long contentLength, String contentType) {}
}
