package com.ims.platform.storage;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ims.storage.type", havingValue = "memory")
public class InMemoryObjectStorage implements ObjectStorage {

  private final Map<String, StoredObjectMeta> objects = new ConcurrentHashMap<>();
  private final Map<String, byte[]> payloads = new ConcurrentHashMap<>();

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
  public void put(String storageKey, String contentType, byte[] bytes) {
    byte[] copy = bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
    payloads.put(storageKey, copy);
    objects.put(storageKey, new StoredObjectMeta(copy.length, contentType));
  }

  @Override
  public Optional<StoredObjectMeta> head(String storageKey) {
    return Optional.ofNullable(objects.get(storageKey));
  }

  /** Test helper — returns a copy of stored bytes when present. */
  public Optional<byte[]> getBytes(String storageKey) {
    byte[] data = payloads.get(storageKey);
    return data == null ? Optional.empty() : Optional.of(Arrays.copyOf(data, data.length));
  }
}
