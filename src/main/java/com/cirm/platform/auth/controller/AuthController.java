package com.cirm.platform.auth.controller;

import com.cirm.platform.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/mock")
    public AuthService.AuthResponse mockLogin(@RequestParam String email) {
        return authService.mockLogin(email);
    }
}
