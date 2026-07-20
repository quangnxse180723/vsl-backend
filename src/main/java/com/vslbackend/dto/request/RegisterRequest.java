package com.vslbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Ten dang nhap khong duoc de trong")
    @Size(min = 3, max = 50, message = "Ten dang nhap phai tu 3 den 50 ky tu")
    private String username;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    @Size(max = 255, message = "Email toi da 255 ky tu")
    private String email;

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 255, message = "Ho ten toi da 255 ky tu")
    private String fullName;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, max = 12, message = "Mat khau phai tu 8 den 12 ky tu")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Mat khau phai chua chu thuong, chu hoa va so"
    )
    private String password;

    @NotBlank(message = "Ma OTP khong duoc de trong")
    private String otp;
}
