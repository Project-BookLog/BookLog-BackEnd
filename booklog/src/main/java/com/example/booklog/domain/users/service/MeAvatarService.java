package com.example.booklog.domain.users.service;

import com.example.booklog.aws.s3.AmazonS3Manager;
import com.example.booklog.domain.users.dto.MeAvatarUpdateResponse;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.Uuid;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import com.example.booklog.global.common.repository.UuidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MeAvatarService {

    private final UsersRepository usersRepository;
    private final UuidRepository uuidRepository;
    private final AmazonS3Manager amazonS3Manager;

    private static final long MAX_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    public MeAvatarUpdateResponse updateAvatar(Long userId, MultipartFile file) {
        validate(file);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_BOOK_NOT_FOUND));

        // 1) Uuid 생성/저장 (Builder 사용)
        Uuid uuid = uuidRepository.save(
                Uuid.builder()
                        .uuid(UUID.randomUUID().toString())
                        .build()
        );

        // 2) S3 key 생성 + 업로드
        String key = amazonS3Manager.generateProfileKeyName(uuid);
        String url = amazonS3Manager.uploadFile(key, file);

        // 3) Users 갱신 (닉네임 유지, 이미지만 변경)
        user.updateProfile(user.getNickname(), url);

        return new MeAvatarUpdateResponse(user.getId(), user.getProfileImageUrl());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new GeneralException(ErrorStatus.FILE_REQUIRED);
        if (file.getSize() > MAX_SIZE) throw new GeneralException(ErrorStatus.FILE_TOO_LARGE);

        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct)) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_IMAGE_TYPE);
        }
    }
}
