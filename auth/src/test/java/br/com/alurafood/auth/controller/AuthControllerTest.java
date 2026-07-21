package br.com.alurafood.auth.controller;

import br.com.alurafood.auth.dto.LoginRequest;
import br.com.alurafood.auth.dto.LoginResponse;
import br.com.alurafood.auth.dto.RegistroRequest;
import br.com.alurafood.auth.service.AuthService;
import br.com.alurafood.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void registrar_deveRetornarTokenComStatus200() throws Exception {
        RegistroRequest request = new RegistroRequest("Admin", "admin@alurafood.com", "123456");
        LoginResponse response = new LoginResponse("token", "Admin", "admin@alurafood.com", "ROLE_USER");
        when(authService.registrar(any(RegistroRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.email").value("admin@alurafood.com"));
    }

    @Test
    void login_deveRetornarTokenComStatus200() throws Exception {
        LoginRequest request = new LoginRequest("admin@alurafood.com", "123456");
        LoginResponse response = new LoginResponse("token", "Admin", "admin@alurafood.com", "ROLE_USER");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void validarToken_deveRetornar200QuandoTokenValido() throws Exception {
        when(jwtService.isTokenValido("token-valido")).thenReturn(true);

        mockMvc.perform(get("/auth/validar").param("token", "token-valido"))
                .andExpect(status().isOk());
    }

    @Test
    void validarToken_deveRetornar401QuandoTokenInvalido() throws Exception {
        when(jwtService.isTokenValido("token-invalido")).thenReturn(false);

        mockMvc.perform(get("/auth/validar").param("token", "token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
