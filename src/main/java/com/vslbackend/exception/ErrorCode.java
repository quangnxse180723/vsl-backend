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
    ACCOUNT_DISABLED("AUTH_1006", "Tai khoan cua ban da bi quan tri vien vo hieu hoa", HttpStatus.FORBIDDEN),

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
    CATEGORY_NOT_FOUND("STORE_5003", "Khong tim thay danh muc", HttpStatus.NOT_FOUND),
    VOCABULARY_ALREADY_EXISTS("STORE_5004", "Tu vung nay da ton tai trong he thong", HttpStatus.CONFLICT),
    VOCABULARY_SUGGESTION_NOT_FOUND("STORE_5005", "Khong tim thay de xuat tu vung", HttpStatus.NOT_FOUND),
    VOCABULARY_SUGGESTION_ALREADY_PENDING("STORE_5006", "Tu vung nay dang co de xuat cho duyet, vui long doi quan tri vien xem xet", HttpStatus.CONFLICT),

    // ----------------------- BLOG (BLOG_6xxx) -----------------------
    BLOG_NOT_FOUND("BLOG_6001", "Khong tim thay bai viet", HttpStatus.NOT_FOUND),
    BLOG_REJECTED("BLOG_6002", "Bai viet khong duoc AI phe duyet", HttpStatus.UNPROCESSABLE_ENTITY),
    MODERATION_ERROR("BLOG_6003", "Khong the kiem duyet bai viet luc nay, vui long thu lai sau", HttpStatus.SERVICE_UNAVAILABLE),
    COMMENT_NOT_FOUND("BLOG_6004", "Khong tim thay binh luan", HttpStatus.NOT_FOUND),
    REPORT_NOT_FOUND("BLOG_6005", "Khong tim thay don to cao", HttpStatus.NOT_FOUND),
    ALREADY_REPORTED("BLOG_6006", "Ban da to cao bai viet nay roi", HttpStatus.CONFLICT),
    CANNOT_REPORT_OWN_BLOG("BLOG_6007", "Ban khong the to cao bai viet cua chinh minh", HttpStatus.BAD_REQUEST),
    BLOG_CONTENT_ALREADY_REPORTED("BLOG_6011", "Noi dung nay da tung bi to cao truoc do, khong the dang lai", HttpStatus.CONFLICT),
    BLOG_DUPLICATE_CONTENT("BLOG_6013", "Da co bai viet cong khai voi noi dung nay, khong the dang trung lap", HttpStatus.CONFLICT),
    BLOG_UNDER_REPORT("BLOG_6012", "Bai viet dang bi to cao va cho quan tri vien xu ly nen khong the xoa. Ban co the chuyen bai ve ban nhap de an khoi trang cong khai.", HttpStatus.CONFLICT),
    CANNOT_FOLLOW_SELF("BLOG_6008", "Ban khong the tu theo doi chinh minh", HttpStatus.BAD_REQUEST),
    NOT_FRIEND("BLOG_6009", "Chi co the chia se qua profile voi ban be", HttpStatus.FORBIDDEN),
    NOTIFICATION_NOT_FOUND("BLOG_6010", "Khong tim thay thong bao", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
