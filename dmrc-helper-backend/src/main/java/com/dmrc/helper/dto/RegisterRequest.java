package com.dmrc.helper.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    private String email;

    private String phone;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
