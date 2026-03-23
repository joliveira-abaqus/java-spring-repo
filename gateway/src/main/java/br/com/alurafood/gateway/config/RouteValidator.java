package br.com.alurafood.gateway.config;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

    public static final List<String> ROTAS_PUBLICAS = List.of(
            "/auth/registro",
            "/auth/login",
            "/auth/validar",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isRotaProtegida =
            request -> ROTAS_PUBLICAS.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
