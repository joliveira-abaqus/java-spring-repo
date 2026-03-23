package br.com.alurafood.auth.controller;

import br.com.alurafood.auth.dto.LoginRequest;
import br.com.alurafood.auth.dto.LoginResponse;
import br.com.alurafood.auth.dto.RegistroRequest;
import br.com.alurafood.auth.service.AuthService;
import br.com.alurafood.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/registro")
    public ResponseEntity<LoginResponse> registrar(@RequestBody @Valid RegistroRequest request) {
        LoginResponse response = authService.registrar(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validar")
    public ResponseEntity<Void> validarToken(@RequestParam String token) {
        if (jwtService.isTokenValido(token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }
}
