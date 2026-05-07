package judamov.sipoh.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.service.impl.JwtServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserValidationFilter extends OncePerRequestFilter {

    private final JwtServiceImpl jwtService;

    public UserValidationFilter(JwtServiceImpl jwtService) {
        this.jwtService = jwtService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String path = request.getRequestURI();

            // Solo se valida si la ruta es parte de la API
            if (path.startsWith("/api")) {
                String token = extractTokenIfPresent(request);

                if (token != null) {
                    String headerUserId = request.getHeader("userId");
                    if (headerUserId == null) {
                        throw new GenericAppException(HttpStatus.BAD_REQUEST, "Header 'userId' es obligatorio");
                    }

                    String tokenUserId = jwtService.getUserIdFromToken(token).toString();

                    if (!headerUserId.equals(tokenUserId)) {
                        throw new GenericAppException(HttpStatus.FORBIDDEN, "El userId del header no coincide con el token");
                    }
                }
            }

            filterChain.doFilter(request, response);

        } catch (GenericAppException ex) {
            response.setStatus(ex.getStatus().value());
            response.setContentType("application/json");
            response.getWriter().write("""
            {
              "timestamp": "%s",
              "status": %d,
              "error": "%s",
              "message": "%s"
            }
        """.formatted(
                    java.time.Instant.now(),
                    ex.getStatus().value(),
                    ex.getStatus().getReasonPhrase(),
                    ex.getMessage()
            ));
        }
    }

    private String extractTokenIfPresent(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

}
