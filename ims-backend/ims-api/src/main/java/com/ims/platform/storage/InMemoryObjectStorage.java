package com.ims.platform.storage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ims.storage.type", havingValue = "memory")
public class InMemoryObjectStorage implements ObjectStorage {

  private final Map<String, StoredObjectMeta> objects = new ConcurrentHashMap<>();

  @Override
  public String presignPut(
      String storageKey, String contentType, long contentLength, Duration expiry) {
    objects.put(storageKey, new StoredObjectMeta(contentLength, contentType));
    return "http://127.0.0.1:9/memory/" + storageKey + "?expiry=" + expiry.toSeconds();
  }

  @Override
  public String presignGet(
      String storageKey, String fileName, String contentType, Duration expiry, boolean attachment) {
    return "http://127.0.0.1:9/memory/"
        + storageKey
        + "?download="
        + fileName
        + "&type="
        + contentType
        + "&expiry="
        + expiry.toSeconds()
        + "&attachment="
        + attachment;
  }

  @Override
  public Optional<StoredObjectMeta> head(String storageKey) {
    return Optional.ofNullable(objects.get(storageKey));
  }
}
