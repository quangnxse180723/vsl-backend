package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Ket qua quet AI tim tu dong nghia cho tinh nang de xuat tu vung.
 * aiChecked=false khi AI bi tat / khong co key -> UI khong hien gi.
 */
@Getter
@Builder
public class VocabularySynonymResponse {

    private final boolean aiChecked;   // AI co duoc bat & goi khong (false = tat/khong co key)
    private final boolean aiError;     // true = da goi nhung that bai (vd het quota) -> UI bao loi
    private final List<SynonymItem> synonyms;

    @Getter
    @Builder
    public static class SynonymItem {
        private final String word;
        private final String categoryName;
    }
}
