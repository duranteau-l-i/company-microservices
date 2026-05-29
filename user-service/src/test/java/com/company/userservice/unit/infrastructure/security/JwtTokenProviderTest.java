package com.company.userservice.unit.infrastructure.security;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.TokenProvider.TokenPair;
import com.company.userservice.security.JwtTokenProvider;
import com.company.userservice.stubs.InMemoryRefreshTokenRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "this-is-a-long-enough-secret-for-hmac-sha-256-signing-key!!";

    private InMemoryRefreshTokenRepository refreshTokenRepo;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        refreshTokenRepo = new InMemoryRefreshTokenRepository();
        provider = new JwtTokenProvider(SECRET, 1_800_000, 604_800_000, refreshTokenRepo);
    }

    @Test
    void issueAndParseAccessToken() {
        UserId id = UserId.generate();
        TokenPair pair = provider.issueTokens(id, "alice@test.com", Role.USER);

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.expiresInSeconds()).isEqualTo(1800);

        Claims claims = provider.parseClaims(pair.accessToken());

        assertThat(claims.getSubject()).isEqualTo(id.value().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("alice@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    void refreshTokenIsOpaqueNotAJwt() {
        TokenPair pair = provider.issueTokens(UserId.generate(), "alice@test.com", Role.USER);

        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(provider.isAccessToken(pair.refreshToken())).isFalse();
    }

    @Test
    void isAccessTokenDistinguishesTypes() {
        TokenPair pair = provider.issueTokens(UserId.generate(), "alice@test.com", Role.USER);

        assertThat(provider.isAccessToken(pair.accessToken())).isTrue();
        assertThat(provider.isAccessToken(pair.refreshToken())).isFalse();
    }

    @Test
    void refreshRotatesTokens() {
        TokenPair original = provider.issueTokens(UserId.generate(), "alice@test.com", Role.MANAGER);

        assertThat(refreshTokenRepo.size()).isEqualTo(1);

        TokenPair refreshed = provider.refresh(original.refreshToken());

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(refreshTokenRepo.size()).isEqualTo(1);

        Claims claims = provider.parseClaims(refreshed.accessToken());
        assertThat(claims.get("role", String.class)).isEqualTo("MANAGER");
    }

    @Test
    void refreshTokenIsOneTimeUse() {
        TokenPair pair = provider.issueTokens(UserId.generate(), "alice@test.com", Role.USER);
        provider.refresh(pair.refreshToken());

        assertThatThrownBy(() -> provider.refresh(pair.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshRejectsUnknownToken() {
        assertThatThrownBy(() -> provider.refresh("unknown-opaque-token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void parseClaimsRejectsGarbage() {
        assertThatThrownBy(() -> provider.parseClaims("not-a-token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void tamperedTokenRejected() {
        TokenPair pair = provider.issueTokens(UserId.generate(), "alice@test.com", Role.USER);
        String tampered = pair.accessToken().substring(0, pair.accessToken().length() - 5) + "xxxxx";

        assertThat(provider.isAccessToken(tampered)).isFalse();
    }
}
