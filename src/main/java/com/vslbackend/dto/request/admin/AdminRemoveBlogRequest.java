package com.vslbackend.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Admin go bai blog (do vi pham) kem ly do gui ve cho tac gia. */
@Data
public class AdminRemoveBlogRequest {
    @NotBlank(message = "Vui long nhap ly do go bai")
    private String reason;
}
