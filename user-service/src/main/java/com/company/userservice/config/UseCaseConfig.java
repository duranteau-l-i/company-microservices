package com.company.userservice.config;

import com.company.userservice.application.command.CreateUserHandler;
import com.company.userservice.application.command.DeleteUserHandler;
import com.company.userservice.application.command.RefreshTokenHandler;
import com.company.userservice.application.command.SignInHandler;
import com.company.userservice.application.command.SignUpHandler;
import com.company.userservice.application.command.UpdateUserHandler;
import com.company.userservice.application.query.GetUserHandler;
import com.company.userservice.application.query.ListUsersHandler;
import com.company.userservice.domain.port.usecases.CreateUserUseCase;
import com.company.userservice.domain.port.usecases.DeleteUserUseCase;
import com.company.userservice.domain.port.usecases.GetUserUseCase;
import com.company.userservice.domain.port.usecases.ListUsersUseCase;
import com.company.userservice.domain.port.usecases.RefreshTokenUseCase;
import com.company.userservice.domain.port.usecases.SignInUseCase;
import com.company.userservice.domain.port.usecases.SignUpUseCase;
import com.company.userservice.domain.port.usecases.UpdateUserUseCase;
import com.company.userservice.domain.port.infrastructure.PasswordHasher;
import com.company.userservice.domain.port.infrastructure.TokenProvider;
import com.company.userservice.domain.port.infrastructure.UserCommandRepository;
import com.company.userservice.domain.port.infrastructure.UserEventPublisher;
import com.company.userservice.domain.port.infrastructure.UserQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public SignUpUseCase signUpUseCase(
            UserCommandRepository commandRepository,
            UserEventPublisher publisher,
            PasswordHasher passwordHasher) {
        return new SignUpHandler(commandRepository, publisher, passwordHasher);
    }

    @Bean
    public SignInUseCase signInUseCase(
            UserCommandRepository commandRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider) {
        return new SignInHandler(commandRepository, passwordHasher, tokenProvider);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(TokenProvider tokenProvider) {
        return new RefreshTokenHandler(tokenProvider);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(
            UserCommandRepository commandRepository,
            UserEventPublisher publisher,
            PasswordHasher passwordHasher) {
        return new CreateUserHandler(commandRepository, publisher, passwordHasher);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(
            UserCommandRepository commandRepository,
            UserEventPublisher publisher) {
        return new UpdateUserHandler(commandRepository, publisher);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(
            UserCommandRepository commandRepository,
            UserEventPublisher publisher) {
        return new DeleteUserHandler(commandRepository, publisher);
    }

    @Bean
    public GetUserUseCase getUserUseCase(UserQueryRepository queryRepository) {
        return new GetUserHandler(queryRepository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserQueryRepository queryRepository) {
        return new ListUsersHandler(queryRepository);
    }
}
