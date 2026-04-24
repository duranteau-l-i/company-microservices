package com.company.userservice.presentation.controller;

import com.company.userservice.domain.port.usecases.RefreshTokenUseCase;
import com.company.userservice.domain.port.usecases.SignInUseCase;
import com.company.userservice.domain.port.usecases.SignUpUseCase;
import com.company.userservice.presentation.controller.dto.AuthResponse;
import com.company.userservice.presentation.controller.dto.RefreshRequest;
import com.company.userservice.presentation.controller.dto.SignInRequest;
import com.company.userservice.presentation.controller.dto.SignUpRequest;
import com.company.userservice.presentation.controller.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Authentication", description = "Public sign-up, sign-in, and token refresh endpoints")
@SecurityRequirements
public class AuthController {

    private final SignUpUseCase signUp;
    private final SignInUseCase signIn;
    private final RefreshTokenUseCase refresh;

    public AuthController(SignUpUseCase signUp, SignInUseCase signIn, RefreshTokenUseCase refresh) {
        this.signUp = signUp;
        this.signIn = signIn;
        this.refresh = refresh;
    }

    @Operation(summary = "Register a new user with the USER role")
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest req) {
        UserResponse response = UserResponse.from(signUp.signUp(
                new SignUpUseCase.Command(req.email(), req.password(), req.firstName(), req.lastName())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Authenticate with email and password, returning an access/refresh token pair")
    @PostMapping("/signin")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest req) {
        return AuthResponse.from(signIn.signIn(new SignInUseCase.Command(req.email(), req.password())));
    }

    @Operation(summary = "Exchange a refresh token for a new access/refresh token pair")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return AuthResponse.from(refresh.refresh(new RefreshTokenUseCase.Command(req.refreshToken())));
    }
}
