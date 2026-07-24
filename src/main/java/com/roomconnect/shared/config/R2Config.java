package com.roomconnect.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configures AWS S3 SDK to talk to Cloudflare R2.
 * R2 is S3-compatible; we just point the endpoint at the account endpoint.
 */
@Configuration
public class R2Config {

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.access-key}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        String safeEndpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint : "http://localhost:9000";
        String safeAccessKey = (accessKey != null && !accessKey.isBlank()) ? accessKey : "dev_access_key";
        String safeSecretKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : "dev_secret_key";

        return S3Client.builder()
                .endpointOverride(URI.create(safeEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(safeAccessKey, safeSecretKey)))
                .region(Region.of("auto"))          // R2 uses "auto"
                .forcePathStyle(true)               // required for R2 compatibility
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        String safeEndpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint : "http://localhost:9000";
        String safeAccessKey = (accessKey != null && !accessKey.isBlank()) ? accessKey : "dev_access_key";
        String safeSecretKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : "dev_secret_key";

        return S3Presigner.builder()
                .endpointOverride(URI.create(safeEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(safeAccessKey, safeSecretKey)))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
