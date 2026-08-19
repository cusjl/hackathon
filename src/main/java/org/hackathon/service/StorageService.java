package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.config.S3Properties;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.exception.BusinessException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client client;
    private final S3Presigner presigner;
    private final S3Properties props;

    public String presignPut(String key, String contentType, Duration duration) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(props.bucketName())
                .key(key)
                .contentType(contentType)
                .build();
        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                .putObjectRequest(request)
                .signatureDuration(duration)
                .build()).url().toExternalForm();
    }

    public String presignGet(String key, String contentType, String downloadName, Duration duration) {
        String disposition = "inline";
        if (downloadName != null) {
            String encoded = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
            disposition = "attachment; filename*=UTF-8''" + encoded;
        }
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(props.bucketName())
                .key(key)
                .responseContentType(contentType)
                .responseContentDisposition(disposition)
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .getObjectRequest(request)
                .signatureDuration(duration)
                .build()).url().toExternalForm();
    }

    public String put(String key, String contentType, byte[] content) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.bucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();
            return client.putObject(request, RequestBody.fromBytes(content)).eTag();
        } catch (S3Exception e) {
            log.error("S3 putObject 异常: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException(ResultCode.STORAGE_ERROR);
        }
    }

    public Optional<HeadObjectResponse> head(String key) {
        try {
            return Optional.of(client.headObject(HeadObjectRequest.builder()
                    .bucket(props.bucketName())
                    .key(key)
                    .build()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return Optional.empty();
            log.error("S3 headObject 异常: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException(ResultCode.STORAGE_ERROR);
        }
    }

    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.bucketName())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.error("S3 deleteObject 异常, key={}, msg={}", key, e.awsErrorDetails().errorMessage());
        }
    }

    public ResponseInputStream<GetObjectResponse> openStream(String key) {
        try {
            return client.getObject(GetObjectRequest.builder()
                    .bucket(props.bucketName())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.error("S3 getObject 异常: {}", e.awsErrorDetails().errorMessage());
            throw new BusinessException(ResultCode.STORAGE_ERROR);
        }
    }
}
