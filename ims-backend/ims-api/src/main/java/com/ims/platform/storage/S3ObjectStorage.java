package com.ims.platform.storage;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@ConditionalOnProperty(name = "ims.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3ObjectStorage implements ObjectStorage {

  private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);

  private final StorageProperties properties;
  private final S3Client client;
  private final S3Presigner presigner;
  private volatile boolean bucketReady;

  public S3ObjectStorage(StorageProperties properties) {
    this.properties = properties;
    AwsBasicCredentials credentials =
        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
    StaticCredentialsProvider provider = StaticCredentialsProvider.create(credentials);
    S3Configuration serviceConfig =
        S3Configuration.builder().pathStyleAccessEnabled(properties.pathStyle()).build();
    URI endpoint = URI.create(properties.endpoint());
    this.client =
        S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.of(properties.region()))
            .credentialsProvider(provider)
            .serviceConfiguration(serviceConfig)
            .build();
    this.presigner =
        S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(Region.of(properties.region()))
            .credentialsProvider(provider)
            .serviceConfiguration(serviceConfig)
            .build();
  }

  @Override
  public String presignPut(
      String storageKey, String contentType, long contentLength, Duration expiry) {
    ensureBucket();
    PutObjectRequest put =
        PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(storageKey)
            .contentType(contentType)
            .contentLength(contentLength)
            .build();
    try {
      return presigner
          .presignPutObject(
              PutObjectPresignRequest.builder()
                  .signatureDuration(expiry)
                  .putObjectRequest(put)
                  .build())
          .url()
          .toString();
    } catch (SdkClientException ex) {
      throw unavailable(ex);
    }
  }

  @Override
  public String presignGet(
      String storageKey, String fileName, String contentType, Duration expiry, boolean attachment) {
    ensureBucket();
    GetObjectRequest get =
        GetObjectRequest.builder()
            .bucket(properties.bucket())
            .key(storageKey)
            .responseContentType(contentType)
            .responseContentDisposition(contentDisposition(fileName, attachment))
            .build();
    try {
      return presigner
          .presignGetObject(
              GetObjectPresignRequest.builder()
                  .signatureDuration(expiry)
                  .getObjectRequest(get)
                  .build())
          .url()
          .toString();
    } catch (SdkClientException ex) {
      throw unavailable(ex);
    }
  }

  @Override
  public Optional<StoredObjectMeta> head(String storageKey) {
    ensureBucket();
    try {
      HeadObjectResponse response =
          client.headObject(
              HeadObjectRequest.builder().bucket(properties.bucket()).key(storageKey).build());
      return Optional.of(
          new StoredObjectMeta(
              response.contentLength() == null ? 0L : response.contentLength(),
              response.contentType()));
    } catch (NoSuchKeyException ex) {
      return Optional.empty();
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return Optional.empty();
      }
      throw unavailable(ex);
    } catch (SdkClientException ex) {
      throw unavailable(ex);
    }
  }

  private void ensureBucket() {
    if (bucketReady) {
      return;
    }
    synchronized (this) {
      if (bucketReady) {
        return;
      }
      try {
        client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
        log.info("Created object-storage bucket={}", properties.bucket());
      } catch (BucketAlreadyOwnedByYouException ex) {
        // already present
      } catch (S3Exception ex) {
        if (ex.statusCode() != 409) {
          throw unavailable(ex);
        }
      } catch (SdkClientException ex) {
        throw unavailable(ex);
      }
      applyCors();
      bucketReady = true;
    }
  }

  private void applyCors() {
    List<String> origins = new ArrayList<>(properties.corsOrigins());
    if (origins.isEmpty()) {
      origins.add("http://localhost:4200");
    }
    CORSRule rule =
        CORSRule.builder()
            .allowedOrigins(origins)
            .allowedMethods("GET", "PUT", "HEAD")
            .allowedHeaders("*")
            .exposeHeaders("ETag", "x-amz-request-id")
            .maxAgeSeconds(3600)
            .build();
    try {
      client.putBucketCors(
          PutBucketCorsRequest.builder()
              .bucket(properties.bucket())
              .corsConfiguration(CORSConfiguration.builder().corsRules(rule).build())
              .build());
    } catch (SdkClientException | S3Exception ex) {
      log.warn("Could not apply CORS on bucket={}: {}", properties.bucket(), ex.getMessage());
    }
  }

  private static String contentDisposition(String fileName, boolean attachment) {
    String safe = fileName.replace("\"", "");
    String kind = attachment ? "attachment" : "inline";
    return kind + "; filename=\"" + safe + "\"";
  }

  private ResponseStatusException unavailable(Exception ex) {
    log.warn("Object storage unavailable: {}", ex.getMessage());
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Object storage is unavailable. Start MinIO (docker compose in infra/) and retry.");
  }
}
