package judamov.sipoh.service;

import judamov.sipoh.entity.Program;
import judamov.sipoh.entity.Role;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IProgramRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.repository.IUserRoleRepository;
import judamov.sipoh.service.impl.JwtServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtServiceImpl - Tests unitarios")
class JwtServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private IUserRoleRepository userRoleRepository;
    @Mock private IProgramRepository programRepository;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private User user;
    private Program program;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "currentSchema", "ing_sistemas");
        user = TestDataFactory.buildAdminUser();
        program = TestDataFactory.buildProgram();
    }

    @Test
    @DisplayName("getToken - genera un token JWT no nulo para un usuario válido")
    void shouldGenerateTokenForValidUser() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(user, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(user, program))
                .thenReturn(Optional.of(List.of(userRol)));

        String token = jwtService.getToken(user);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("getToken - el token contiene el documento del usuario")
    void shouldContainDocumentoInToken() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(user, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(user, program))
                .thenReturn(Optional.of(List.of(userRol)));

        String token = jwtService.getToken(user);
        String documento = jwtService.getUsernameFromToken(token);

        assertThat(documento).isEqualTo(user.getDocumento());
    }

    @Test
    @DisplayName("getToken - lanza excepción cuando el programa no existe")
    void shouldThrowWhenProgramNotFound() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jwtService.getToken(user))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Programa no encontrado");
    }

    @Test
    @DisplayName("isTokenValid - retorna true para token válido y usuario correcto")
    void shouldReturnTrueForValidToken() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(user, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(user, program))
                .thenReturn(Optional.of(List.of(userRol)));

        String token = jwtService.getToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - lanza excepción cuando el token no corresponde al usuario")
    void shouldThrowWhenTokenDoesNotMatchUser() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(user, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(user, program))
                .thenReturn(Optional.of(List.of(userRol)));

        String token = jwtService.getToken(user);
        User otherUser = TestDataFactory.buildTeacherUser();

        assertThatThrownBy(() -> jwtService.isTokenValid(token, otherUser))
                .isInstanceOf(GenericAppException.class);
    }

    @Test
    @DisplayName("hashToken - genera un hash SHA-256 no nulo y con longitud de 64 caracteres hex")
    void shouldGenerateHashOfCorrectLength() {
        String token = "some.jwt.token";
        String hash = jwtService.hashToken(token);

        assertThat(hash).isNotNull().hasSize(64);
    }

    @Test
    @DisplayName("hashToken - el mismo token produce siempre el mismo hash")
    void shouldProduceDeterministicHash() {
        String token = "stable.token.value";
        assertThat(jwtService.hashToken(token)).isEqualTo(jwtService.hashToken(token));
    }

    @Test
    @DisplayName("getRolesFromToken - retorna los roles incluidos en el token")
    void shouldReturnRolesFromToken() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(user, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(user, program))
                .thenReturn(Optional.of(List.of(userRol)));

        String token = jwtService.getToken(user);
        List<String> roles = jwtService.getRolesFromToken(token);

        assertThat(roles).contains("DIRECTOR DE ESCUELA");
    }

    @Test
    @DisplayName("getUserIdFromToken - retorna el id del usuario embebido en el token")
    void shouldReturnUserIdFromToken() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(user, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(user, program))
                .thenReturn(Optional.of(List.of(userRol)));

        String token = jwtService.getToken(user);
        Integer userId = jwtService.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(user.getId().intValue());
    }
}
