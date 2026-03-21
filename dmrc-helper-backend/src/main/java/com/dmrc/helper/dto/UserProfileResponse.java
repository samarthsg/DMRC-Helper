package com.dmrc.helper.dto;

import com.dmrc.helper.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String email;
    private String phone;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private User.RegistrationType registrationType;
    private LocalDateTime createdAt;
}
