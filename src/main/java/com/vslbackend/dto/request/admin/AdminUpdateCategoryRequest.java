package com.vslbackend.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpdateCategoryRequest {
    @NotBlank(message = "Ten category khong duoc de trong")
    private String name;

    private String description;
}
