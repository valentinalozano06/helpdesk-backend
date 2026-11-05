package com.morales.helpdesk.service;

import com.morales.helpdesk.dto.auth.LoginRequest;
import com.morales.helpdesk.dto.auth.LoginResponse;
import com.morales.helpdesk.dto.auth.RegisterRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse registrar(RegisterRequest request);
}
