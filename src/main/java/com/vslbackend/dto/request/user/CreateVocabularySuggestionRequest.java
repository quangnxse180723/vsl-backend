package com.vslbackend.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Nguoi dung de xuat mot tu vung moi: danh muc + ten tu vung (+ mo ta tuy chon). */
@Data
public class CreateVocabularySuggestionRequest {

    @NotNull(message = "Vui long chon danh muc")
    private Long categoryId;

    @NotBlank(message = "Vui long nhap ten tu vung")
    @Size(max = 255, message = "Ten tu vung toi da 255 ky tu")
    private String word;

    @Size(max = 255, message = "Mo ta toi da 255 ky tu")
    private String description;
}
