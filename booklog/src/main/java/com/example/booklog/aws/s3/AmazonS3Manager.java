package com.example.booklog.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.booklog.global.common.Uuid;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import com.example.booklog.global.common.repository.UuidRepository;
import com.example.booklog.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Profile("!local")
@Component
@RequiredArgsConstructor
public class AmazonS3Manager {

    private final AmazonS3 amazonS3;

    private final AmazonConfig amazonConfig;

    private final UuidRepository uuidRepository;

    public String uploadFile(String keyName, MultipartFile file){
        String bucket = amazonConfig.getBucket();
        log.info("[S3] upload start bucket='{}' key='{}' size={} contentType={}",
                bucket, keyName, file.getSize(), file.getContentType());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());

        try {
            amazonS3.putObject(new PutObjectRequest(bucket, keyName, file.getInputStream(), metadata));
        } catch (Exception e) {
            log.error("[S3] putObject failed bucket='{}' key='{}'", bucket, keyName, e);
            throw new GeneralException(ErrorStatus.FILE_UPLOAD_FAILED);
        }

        String url = amazonS3.getUrl(bucket, keyName).toString();
        log.info("[S3] upload success url='{}'", url);
        return url;
    }

    public String generateReviewKeyName(Uuid uuid) {
        return amazonConfig.getReviewPath() + '/' + uuid.getUuid();
    }

    public String generateProfileKeyName(Uuid uuid) {
        return amazonConfig.getProfilePath() + '/' + uuid.getUuid();
    }

    public String generateBooklogKeyName(Uuid uuid) {
        return amazonConfig.getBooklogPath() + '/' + uuid.getUuid();
    }

}
