package com.dmrc.helper.repository;

import com.dmrc.helper.entity.User;
import com.dmrc.helper.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenAndTokenType(
            String token, VerificationToken.TokenType tokenType);

    Optional<VerificationToken> findByUserAndTokenTypeAndUsedFalse(
            User user, VerificationToken.TokenType tokenType);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
