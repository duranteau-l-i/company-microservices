package com.company.userservice.domain.port.usecases;

import com.company.userservice.domain.model.UserReadModel;

public interface SignUpUseCase {
    UserReadModel signUp(Command command);

    record Command(String email, String password, String firstName, String lastName) {}
}
