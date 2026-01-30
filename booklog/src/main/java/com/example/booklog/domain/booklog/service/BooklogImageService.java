package com.example.booklog.domain.booklog.service;

import com.example.booklog.aws.s3.AmazonS3Manager;
import com.example.booklog.domain.booklog.dto.BooklogImageUploadResponse;
import com.example.booklog.global.common.Uuid;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import com.example.booklog.global.common.repository.UuidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Profile("!local")
@Service
@RequiredArgsConstructor
public class BooklogImageService {

    private final AmazonS3Manager amazonS3Manager;
    private final UuidRepository uuidRepository;

    // 허용할 이미지 타입(최소한의 방어)
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public BooklogImageUploadResponse uploadBooklogImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ErrorStatus.FILE_REQUIRED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_IMAGE_TYPE);
        }
        // 1) UUID 발급/저장 (프로젝트에서 쓰는 Uuid 테이블/레포 그대로 활용)
        Uuid uuid = uuidRepository.save(Uuid.create());

        // 2) S3 key 생성
        String keyName = amazonS3Manager.generateBooklogKeyName(uuid);


        // 3) 업로드 → public URL 반환
        String imageUrl = amazonS3Manager.uploadFile(keyName, file);

        return BooklogImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .build();
    }
}
