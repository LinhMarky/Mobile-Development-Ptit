package com.homely.rental.auth.service;

import com.homely.rental.auth.dto.request.RegisterRequest;
import com.homely.rental.auth.dto.response.UserResponse;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and verification-token creation share one database transaction. */
@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserService users;
    private final EmailService emails;

    @Transactional(rollbackFor = Exception.class)
    public UserResponse register(RegisterRequest request) throws IdInvalidException {
        UserResponse response = users.registerUser(request);
        emails.generateVerificationToken(users.handleGetUserByUsername(request.getEmail()));
        return response;
    }
}
