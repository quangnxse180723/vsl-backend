package com.vslbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Danh muc ma loi chuan cua he thong.
 * <p>
 * Moi loi gom 3 thanh phan:
 * <ul>
 *     <li>{@code code}    - ma loi nghiep vu duy nhat (VD: AUTH_1001), tien cho client xu ly.</li>
 *     <li>{@code message} - thong diep mac dinh, than thien voi nguoi dung.</li>
 *     <li>{@code status}  - HTTP status tra ve.</li>
 * </ul>
 * Quy uoc nhom ma:
 * <pre>
 *   COMMON_9xxx : loi chung / he thong
 *   AUTH_1xxx   : xac thuc & dang ky
 *   TOKEN_2xxx  : access / refresh token
 *   USER_3xxx   : nguoi dung
 * </pre>
 */
@Getter
public enum ErrorCode {

    // ----------------------- COMMON / SYSTEM -----------------------
    INTERNAL_ERROR("COMMON_9000", "Da co loi xay ra, vui long thu lai sau", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("COMMON_9001", "Du lieu gui len khong hop le", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("COMMON_9002", "Yeu cau khong hop le", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("COMMON_9003", "Phuong thuc HTTP khong duoc ho tro", HttpStatus.METHOD_NOT_ALLOWED),
    RESOURCE_NOT_FOUND("COMMON_9004", "Khong tim thay tai nguyen", HttpStatus.NOT_FOUND),

    // ----------------------- AUTH -----------------------
    EMAIL_ALREADY_EXISTS("AUTH_1001", "Email da duoc su dung", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS("AUTH_1002", "Ten dang nhap da ton tai", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("AUTH_1003", "Email hoac mat khau khong dung", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED("AUTH_1004", "Ban can dang nhap de truy cap tai nguyen nay", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_1005", "Ban khong co quyen truy cap tai nguyen nay", HttpStatus.FORBIDDEN),

    // ----------------------- TOKEN -----------------------
    INVALID_TOKEN("TOKEN_2001", "Token khong hop le", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_2002", "Token da het han", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("TOKEN_2003", "Refresh token khong ton tai", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("TOKEN_2004", "Refresh token da het han, vui long dang nhap lai", HttpStatus.UNAUTHORIZED),

    // ----------------------- USER -----------------------
    USER_NOT_FOUND("USER_3001", "Khong tim thay nguoi dung", HttpStatus.NOT_FOUND),

    // ----------------------- AI / VIDEO (AI_4xxx) -----------------------
    VIDEO_EMPTY("AI_4001", "File video khong duoc de trong", HttpStatus.BAD_REQUEST),
    VIDEO_INVALID_TYPE("AI_4002", "File tai len phai la video (mp4, webm, mov, ...)", HttpStatus.BAD_REQUEST),
    VIDEO_CORRUPT("AI_4003", "Video bi hong hoac dinh dang khong duoc ho tro", HttpStatus.UNPROCESSABLE_ENTITY),
    VIDEO_NO_FRAMES("AI_4004", "Khong the doc frame tu video", HttpStatus.UNPROCESSABLE_ENTITY),
    AI_MODEL_NOT_LOADED("AI_4005", "Mo hinh AI chua san sang, vui long lien he quan tri vien", HttpStatus.SERVICE_UNAVAILABLE),
    AI_INFERENCE_ERROR("AI_4006", "Loi trong qua trinh phan tich video", HttpStatus.INTERNAL_SERVER_ERROR),

    // ----------------------- MINIO / STORAGE (STORE_5xxx) -----------------------
    MINIO_UPLOAD_ERROR("STORE_5001", "Khong the tai video len he thong luu tru", HttpStatus.INTERNAL_SERVER_ERROR),
    VOCABULARY_NOT_FOUND("STORE_5002", "Khong tim thay tu vung", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("STORE_5003", "Khong tim thay danh muc", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
