package com.vslbackend.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "Vui long nhap ly do to cao")
    private String reason;
}
