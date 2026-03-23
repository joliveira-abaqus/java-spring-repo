package br.com.alurafood.auth.service;

import br.com.alurafood.auth.dto.LoginRequest;
import br.com.alurafood.auth.dto.LoginResponse;
import br.com.alurafood.auth.dto.RegistroRequest;
import br.com.alurafood.auth.model.Role;
import br.com.alurafood.auth.model.Usuario;
import br.com.alurafood.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService implements UserDetailsService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
    }

    public LoginResponse registrar(RegistroRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email ja cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(Role.ROLE_USER);
        usuario.setAtivo(true);
        repository.save(usuario);

        String token = jwtService.gerarToken(
                Map.of("role", usuario.getRole().name()),
                usuario
        );

        return new LoginResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getRole().name());
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        Usuario usuario = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));

        String token = jwtService.gerarToken(
                Map.of("role", usuario.getRole().name()),
                usuario
        );

        return new LoginResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getRole().name());
    }
}
