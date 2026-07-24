package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Ket qua kiem tra tu vung da ton tai hay chua (dung cho tinh nang de xuat tu vung).
 * Neu ton tai, kem theo danh muc ma tu do dang thuoc ve de hien thi thong bao.
 */
@Getter
@Builder
public class VocabularyExistsResponse {
    private final boolean exists;
    /** Cach viet chuan (co dau) cua tu da ton tai - de UI hien dung chinh ta du nguoi dung go khong dau. */
    private final String word;
    private final Long categoryId;
    private final String categoryName;
}
