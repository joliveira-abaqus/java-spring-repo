package br.com.alurafood.gateway.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

    private static final List<Pattern> PADROES_ROTAS_PUBLICAS = List.of(
            Pattern.compile("^(/[^/]+)?/auth/registro(/.*)?$"),
            Pattern.compile("^(/[^/]+)?/auth/login(/.*)?$"),
            Pattern.compile("^(/[^/]+)?/auth/validar(/.*)?$"),
            Pattern.compile("^(/[^/]+)?/eureka(/.*)?$")
    );

    public Predicate<ServerHttpRequest> isRotaProtegida =
            request -> PADROES_ROTAS_PUBLICAS.stream()
                    .noneMatch(pattern -> pattern.matcher(request.getURI().getPath()).matches());
}
