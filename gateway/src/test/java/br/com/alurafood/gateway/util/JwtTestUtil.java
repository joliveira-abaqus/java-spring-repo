package br.com.alurafood.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Utilitario para geracao de tokens JWT em testes.
 */
public final class JwtTestUtil {

    public static final String SECRET = "YWx1cmFmb29kLXNlY3JldC1rZXktand0LXNlY3VyaXR5LTI1Ni1iaXRzLW1pbmltdW0=";

    private JwtTestUtil() {
    }

    public static String gerarToken(String email, String role) {
        return gerarToken(email, role, System.currentTimeMillis() + 3_600_000);
    }

    public static String gerarToken(String email, String role, long expiracaoMs) {
        SecretKey chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .claims(Map.of("role", role))
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(expiracaoMs))
                .signWith(chave)
                .compact();
    }

    public static String gerarTokenExpirado(String email, String role) {
        return gerarToken(email, role, System.currentTimeMillis() - 60_000);
    }

    public static String gerarTokenComChaveInvalida(String email, String role) {
        SecretKey chaveErrada = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode("b3V0cmEtY2hhdmUtc2VjcmV0YS1xdWUtbmFvLWJhdGUtY29tLWEtb3JpZ2luYWw="));
        return Jwts.builder()
                .claims(Map.of("role", role))
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(chaveErrada)
                .compact();
    }
}
