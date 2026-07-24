package com.vslbackend.util;

import java.text.Normalizer;

/**
 * Chuan hoa van ban tieng Viet de SO KHOP (khong dung de hien thi).
 * Dung chung cho nhieu mien nghiep vu: tu vung (tim tu trung) va blog (chan dang lai
 * noi dung da bi to cao) - nen tach rieng thay vi de trong mot service cu the.
 */
public final class VietnameseText {

    private VietnameseText() {}

    /**
     * Bo dau tieng Viet + ha chu thuong + gom khoang trang de so khop khong phan biet
     * dau / hoa-thuong / cach trinh bay.
     * NFD tach dau thanh & dau mu roi xoa; rieng cac chu KHONG tach bang NFD (đ) map tay.
     * Vi du: "con chuột" -> "con chuot", "Sợ  Hãi" -> "so hai".
     */
    public static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replace('ơ', 'o').replace('Ơ', 'O')
                .replace('ư', 'u').replace('Ư', 'U');
        return n.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
