package com.company.userservice.infrastructure.adapter.in.rest;

import com.company.userservice.domain.port.in.RefreshTokenUseCase;
import com.company.userservice.domain.port.in.SignInUseCase;
import com.company.userservice.domain.port.in.SignUpUseCase;
import com.company.userservice.infrastructure.adapter.in.rest.dto.AuthResponse;
import com.company.userservice.infrastructure.adapter.in.rest.dto.RefreshRequest;
import com.company.userservice.infrastructure.adapter.in.rest.dto.SignInRequest;
import com.company.userservice.infrastructure.adapter.in.rest.dto.SignUpRequest;
import com.company.userservice.infrastructure.adapter.in.rest.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final SignUpUseCase signUp;
    private final SignInUseCase signIn;
    private final RefreshTokenUseCase refresh;

    public AuthController(SignUpUseCase signUp, SignInUseCase signIn, RefreshTokenUseCase refresh) {
        this.signUp = signUp;
        this.signIn = signIn;
        this.refresh = refresh;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest req) {
        UserResponse response = UserResponse.from(signUp.signUp(
                new SignUpUseCase.Command(req.email(), req.password(), req.firstName(), req.lastName())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signin")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest req) {
        return AuthResponse.from(signIn.signIn(new SignInUseCase.Command(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return AuthResponse.from(refresh.refresh(new RefreshTokenUseCase.Command(req.refreshToken())));
    }
}
