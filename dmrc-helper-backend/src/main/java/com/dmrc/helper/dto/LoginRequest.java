package com.dmrc.helper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Login ID (email or phone) is required")
    private String loginId;

    @NotBlank(message = "Password is required")
    private String password;
}
