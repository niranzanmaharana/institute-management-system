package com.ims.platform.storage;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ims.storage")
public record StorageProperties(
    @DefaultValue("s3") String type,
    @DefaultValue("http://localhost:9000") String endpoint,
    @DefaultValue("minio") String accessKey,
    @DefaultValue("minio12345") String secretKey,
    @DefaultValue("ims-private") String bucket,
    @DefaultValue("us-east-1") String region,
    @DefaultValue("true") boolean pathStyle,
    @DefaultValue("10") int presignExpiryMinutes,
    @DefaultValue("5242880") long maxSizeBytes,
    @DefaultValue({"http://localhost:4200", "http://127.0.0.1:4200"}) List<String> corsOrigins) {}
