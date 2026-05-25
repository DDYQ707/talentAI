package com.talent.resume.service;

import com.talent.resume.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public StoredObject upload(MultipartFile file, Long candidateId) throws Exception {
        ensureBucket();
        String bucket = minioProperties.getBucketName();
        String objectKey = buildObjectKey(candidateId, file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        }
        String fileUrl = buildPublicUrl(bucket, objectKey);
        return new StoredObject(bucket, objectKey, fileUrl);
    }

    public String getPresignedUrl(String bucket, String objectKey, int expireHours) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectKey)
                        .expiry(expireHours, TimeUnit.HOURS)
                        .build());
    }

    /**
     * 浏览器内联预览（PDF 等），私有桶通过预签名访问。
     */
    public String getPresignedPreviewUrl(
            String bucket, String objectKey, String fileName, String fileType, int expireHours) throws Exception {
        if (!StringUtils.hasText(bucket) || !StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("附件存储信息不完整，无法预览");
        }
        return getPresignedUrl(bucket, objectKey, expireHours);
    }

    private String encodeFilename(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "resume";
        }
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String resolveContentType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return "application/octet-stream";
        }
        return switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private void ensureBucket() throws Exception {
        String bucket = minioProperties.getBucketName();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String buildObjectKey(Long candidateId, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename);
        String date = LocalDate.now().format(DATE_FMT);
        return date + "/c" + candidateId + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "resume.bin";
        }
        String name = filename.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private String buildPublicUrl(String bucket, String objectKey) {
        String endpoint = minioProperties.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + bucket + "/" + objectKey;
    }

    public record StoredObject(String bucketName, String objectKey, String fileUrl) {
    }
}
