package com.vslbackend.service;

import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.tutorials}")
    private String tutorialBucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${minio.bucket.avatars}")
    private String avatarBucket;

    /**
     * Tao bucket neu chua ton tai va cau hinh chinh sach doc cong khai.
     * Loi MinIO khong cat startup cua ung dung - chi log warning.
     */
    @PostConstruct
    public void initBuckets() {
        initBucket(tutorialBucket);
        initBucket(avatarBucket);
    }

    private void initBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build());

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build());

                setPublicReadPolicy(bucket);

                log.info("Created bucket {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Init bucket {} failed: {}", bucket, e.getMessage());
        }
    }

    /**
     * Upload video bai hoc mau len MinIO.
     *
     * @param file       file tu admin
     * @param objectName ten object trong bucket (vd: "category1/word_hello.mp4")
     * @return URL cong khai ma frontend dung de stream video
     */
    public String uploadTutorialVideo(MultipartFile file, String objectName) {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(tutorialBucket)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "video/mp4")
                            .build());

            String url = buildPublicUrl(tutorialBucket, objectName);
            log.info("Uploaded tutorial video: {} -> {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("MinIO upload failed for object '{}': {}", objectName, e.getMessage(), e);
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Loi tai len: " + e.getMessage());
        }
    }

    /**
     * Tao ten object duy nhat cho video: {vocabularyId}/{uuid}.mp4
     */
    public String generateObjectName(Long vocabularyId, String originalFilename) {
        String ext = extractExtension(originalFilename);
        return "vocabulary-" + vocabularyId + "/" + UUID.randomUUID() + ext;
    }

    /**
     * Xoa object khoi bucket (dung khi thay the video mau).
     */
    public void deleteObject(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(tutorialBucket)
                            .object(objectName)
                            .build());
            log.info("Deleted MinIO object: {}", objectName);
        } catch (Exception e) {
            log.warn("Could not delete MinIO object '{}': {}", objectName, e.getMessage());
        }
    }

    /**
     * Xoa video tutorial dua tren public URL da luu trong Vocabulary.videoTutorialUrl.
     * Bo qua neu url null/rong hoac khong khop dinh dang public URL cua bucket nay.
     */
    public void deleteTutorialVideoByUrl(String url) {
        if (url == null || url.isBlank()) return;

        String marker = "/" + tutorialBucket + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) return;

        String objectName = url.substring(idx + marker.length());
        deleteObject(objectName);
    }

    /**
     * Presigned URL co han (7 ngay) cho object trong bucket private.
     * Hien tai bucket tutorial la public, method nay du phong cho bucket private.
     */
    public String getPresignedUrl(String bucketName, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build());
        } catch (Exception e) {
            log.error("Could not generate presigned URL for {}/{}: {}", bucketName, objectName, e.getMessage());
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Khong the tao URL: " + e.getMessage());
        }
    }

    public String uploadAvatar(MultipartFile file) {

        try (InputStream is = file.getInputStream()) {

            String objectName =
                    UUID.randomUUID()
                            + "-"
                            + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(avatarBucket)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return buildPublicUrl(avatarBucket, objectName);

        } catch (Exception e) {
            throw new AppException(
                    ErrorCode.MINIO_UPLOAD_ERROR,
                    "Upload avatar failed");
        }
    }

    // ------------------------------------------------------------------

    private String buildPublicUrl(String bucket, String objectName) {
        return publicEndpoint.replaceAll("/$", "") + "/" + bucket + "/" + objectName;
    }

    private void setPublicReadPolicy(String bucket) throws Exception {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".mp4";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
