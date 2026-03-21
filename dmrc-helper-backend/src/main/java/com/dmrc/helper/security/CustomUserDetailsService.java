package com.dmrc.helper.security;

import com.dmrc.helper.entity.User;
import com.dmrc.helper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(loginId)
                .or(() -> userRepository.findByPhone(loginId))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with loginId: " + loginId));

        return new org.springframework.security.core.userdetails.User(
                getLoginId(user),
                user.getPasswordHash(),
                Collections.emptyList()
        );
    }

    private String getLoginId(User user) {
        return user.getRegistrationType() == User.RegistrationType.EMAIL
                ? user.getEmail()
                : user.getPhone();
    }
}
