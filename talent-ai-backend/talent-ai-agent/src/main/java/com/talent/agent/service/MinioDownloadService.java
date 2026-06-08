package com.talent.agent.service;

import com.talent.agent.exception.AgentBusinessException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MinioDownloadService {

    private final MinioClient minioClient;

    public byte[] download(String bucketName, String objectKey) {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            throw new AgentBusinessException("附件存储信息不完整，无法下载");
        }
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectKey).build())) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new AgentBusinessException("从 MinIO 下载附件失败: " + e.getMessage(), e);
        }
    }
}
