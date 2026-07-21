package br.com.alurafood.auth.service;

import br.com.alurafood.auth.dto.LoginRequest;
import br.com.alurafood.auth.dto.LoginResponse;
import br.com.alurafood.auth.dto.RegistroRequest;
import br.com.alurafood.auth.model.Role;
import br.com.alurafood.auth.model.Usuario;
import br.com.alurafood.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Admin");
        usuario.setEmail("admin@alurafood.com");
        usuario.setSenha("senha-codificada");
        usuario.setRole(Role.ROLE_USER);
        usuario.setAtivo(true);
    }

    @Test
    void loadUserByUsername_deveRetornarUsuarioQuandoExiste() {
        when(repository.findByEmail("admin@alurafood.com")).thenReturn(Optional.of(usuario));

        UserDetails result = authService.loadUserByUsername("admin@alurafood.com");

        assertEquals("admin@alurafood.com", result.getUsername());
    }

    @Test
    void loadUserByUsername_deveLancarExcecaoQuandoNaoExiste() {
        when(repository.findByEmail("naoexiste@alurafood.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> authService.loadUserByUsername("naoexiste@alurafood.com"));
    }

    @Test
    void registrar_deveCriarUsuarioERetornarToken() {
        RegistroRequest request = new RegistroRequest("Admin", "admin@alurafood.com", "123456");
        when(repository.existsByEmail("admin@alurafood.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("senha-codificada");
        when(jwtService.gerarToken(anyMap(), any(UserDetails.class))).thenReturn("token-gerado");

        LoginResponse response = authService.registrar(request);

        assertEquals("token-gerado", response.getToken());
        assertEquals("Admin", response.getNome());
        assertEquals("admin@alurafood.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());
        verify(repository).save(any(Usuario.class));
    }

    @Test
    void registrar_deveLancarExcecaoQuandoEmailJaCadastrado() {
        RegistroRequest request = new RegistroRequest("Admin", "admin@alurafood.com", "123456");
        when(repository.existsByEmail("admin@alurafood.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registrar(request));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void login_deveAutenticarERetornarToken() {
        LoginRequest request = new LoginRequest("admin@alurafood.com", "123456");
        when(repository.findByEmail("admin@alurafood.com")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(anyMap(), any(UserDetails.class))).thenReturn("token-login");

        LoginResponse response = authService.login(request);

        assertEquals("token-login", response.getToken());
        assertEquals("admin@alurafood.com", response.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_deveLancarExcecaoQuandoUsuarioNaoEncontradoAposAutenticar() {
        LoginRequest request = new LoginRequest("admin@alurafood.com", "123456");
        when(repository.findByEmail("admin@alurafood.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }
}
