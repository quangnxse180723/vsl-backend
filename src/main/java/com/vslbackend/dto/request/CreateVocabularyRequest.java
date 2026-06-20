package com.vslbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVocabularyRequest {

    @NotNull(message = "categoryId khong duoc de trong")
    private Long categoryId;

    @NotBlank(message = "Tu vung khong duoc de trong")
    @Size(max = 255, message = "Tu vung toi da 255 ky tu")
    private String word;

    @Size(max = 255, message = "Mo ta toi da 255 ky tu")
    private String description;
}
