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

import java.io.File;
import java.io.FileInputStream;
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

    @Value("${minio.bucket.vocabulary-images}")
    private String vocabularyImageBucket;

    @Value("${minio.bucket.category-images}")
    private String categoryImageBucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    /**
     * Tao cac bucket neu chua ton tai va cau hinh chinh sach doc cong khai.
     * Loi MinIO khong cat startup cua ung dung - chi log warning.
     */
    @PostConstruct
    public void initBucket() {
        ensureBucketWithPublicRead(tutorialBucket);
        ensureBucketWithPublicRead(vocabularyImageBucket);
        ensureBucketWithPublicRead(categoryImageBucket);
    }

    private void ensureBucketWithPublicRead(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                setPublicReadPolicy(bucket);
                log.info("MinIO: Created bucket '{}' with public-read policy.", bucket);
            } else {
                log.info("MinIO: Bucket '{}' already exists.", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO initialization failed for bucket '{}' ({}). Upload will be unavailable until MinIO is reachable.", bucket, e.getMessage());
        }
    }

    /**
     * Upload video bai hoc mau (da transcode sang H.264/AAC) len MinIO.
     *
     * @param file       file video tam da qua VideoTranscodingService
     * @param objectName ten object trong bucket (vd: "category1/word_hello.mp4")
     * @return URL cong khai ma frontend dung de stream video
     */
    public String uploadTutorialVideo(File file, String objectName) {
        try (InputStream is = new FileInputStream(file)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(tutorialBucket)
                            .object(objectName)
                            .stream(is, file.length(), -1)
                            .contentType("video/mp4")
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
     * Upload anh minh hoa tu vung len bucket rieng (vocabulary-images).
     *
     * @param file       anh tu admin
     * @param objectName ten object trong bucket (vd: "vocabulary-12/uuid.jpg")
     * @return URL cong khai ma frontend dung de hien thi anh
     */
    public String uploadVocabularyImage(MultipartFile file, String objectName) {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(vocabularyImageBucket)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                            .build());

            String url = buildPublicUrl(vocabularyImageBucket, objectName);
            log.info("Uploaded vocabulary image: {} -> {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("MinIO upload failed for object '{}': {}", objectName, e.getMessage(), e);
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Loi tai len anh: " + e.getMessage());
        }
    }

    /**
     * Tao ten object duy nhat cho anh: vocabulary-{vocabularyId}/{uuid}.{ext}
     */
    public String generateImageObjectName(Long vocabularyId, String originalFilename) {
        String ext = extractExtension(originalFilename);
        return "vocabulary-" + vocabularyId + "/" + UUID.randomUUID() + ext;
    }

    /**
     * Xoa anh tu vung dua tren public URL da luu trong Vocabulary.imageUrl.
     * Bo qua neu url null/rong hoac khong khop dinh dang public URL cua bucket nay.
     */
    public void deleteVocabularyImageByUrl(String url) {
        if (url == null || url.isBlank()) return;

        String marker = "/" + vocabularyImageBucket + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) return;

        String objectName = url.substring(idx + marker.length());
        deleteVocabularyImage(objectName);
    }

    private void deleteVocabularyImage(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(vocabularyImageBucket)
                            .object(objectName)
                            .build());
            log.info("Deleted MinIO vocabulary image: {}", objectName);
        } catch (Exception e) {
            log.warn("Could not delete MinIO vocabulary image '{}': {}", objectName, e.getMessage());
        }
    }

    /**
     * Upload anh bia (cover) cho category len bucket rieng (category-images).
     *
     * @param file       anh tu admin
     * @param objectName ten object trong bucket (vd: "category-3/uuid.jpg")
     * @return URL cong khai ma frontend dung de hien thi anh
     */
    public String uploadCategoryImage(MultipartFile file, String objectName) {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(categoryImageBucket)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                            .build());

            String url = buildPublicUrl(categoryImageBucket, objectName);
            log.info("Uploaded category image: {} -> {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("MinIO upload failed for object '{}': {}", objectName, e.getMessage(), e);
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Loi tai len anh: " + e.getMessage());
        }
    }

    /**
     * Tao ten object duy nhat cho anh bia category: category-{categoryId}/{uuid}.{ext}
     */
    public String generateCategoryImageObjectName(Long categoryId, String originalFilename) {
        String ext = extractExtension(originalFilename);
        return "category-" + categoryId + "/" + UUID.randomUUID() + ext;
    }

    /**
     * Xoa anh bia category dua tren public URL da luu trong Category.imageUrl.
     * Bo qua neu url null/rong hoac khong khop dinh dang public URL cua bucket nay.
     */
    public void deleteCategoryImageByUrl(String url) {
        if (url == null || url.isBlank()) return;

        String marker = "/" + categoryImageBucket + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) return;

        String objectName = url.substring(idx + marker.length());
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(categoryImageBucket)
                            .object(objectName)
                            .build());
            log.info("Deleted MinIO category image: {}", objectName);
        } catch (Exception e) {
            log.warn("Could not delete MinIO category image '{}': {}", objectName, e.getMessage());
        }
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
