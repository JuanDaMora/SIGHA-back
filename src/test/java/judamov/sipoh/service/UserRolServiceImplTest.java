package judamov.sipoh.service;

import judamov.sipoh.entity.Program;
import judamov.sipoh.entity.Role;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IProgramRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.repository.IUserRoleRepository;
import judamov.sipoh.service.impl.UserRolServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRolServiceImpl - Tests unitarios")
class UserRolServiceImplTest {

    @Mock private IUserRoleRepository userRoleRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IProgramRepository programRepository;

    @InjectMocks
    private UserRolServiceImpl userRolService;

    private Program program;
    private User adminUser;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userRolService, "currentSchema", "ing_sistemas");
        program = TestDataFactory.buildProgram();
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
    }

    @Test
    @DisplayName("getRolListFromUser - retorna lista de roles del usuario para el programa actual")
    void shouldReturnRoleListForCurrentProgram() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(adminUser, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        List<Role> roles = userRolService.getRolListFromUser(adminUser);

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getName()).isEqualTo("DIRECTOR DE ESCUELA");
    }

    @Test
    @DisplayName("getRolListFromUser - retorna lista vacía si el usuario no tiene roles en el programa")
    void shouldReturnEmptyListWhenNoRoles() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program))
                .thenReturn(Optional.empty());

        List<Role> roles = userRolService.getRolListFromUser(adminUser);

        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("getRolListFromUser - lanza excepción cuando el programa no existe")
    void shouldThrowWhenProgramNotFound() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRolService.getRolListFromUser(adminUser))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Programa no encontrado");
    }

    @Test
    @DisplayName("hasAdminPrivileges - retorna true para usuario con rol DIRECTOR DE ESCUELA")
    void shouldReturnTrueForDirector() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(adminUser, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        assertThat(userRolService.hasAdminPrivileges(adminUser)).isTrue();
    }

    @Test
    @DisplayName("hasAdminPrivileges - retorna true para usuario con rol COORDINADOR ACADEMICO")
    void shouldReturnTrueForCoordinator() {
        Role coordRole = TestDataFactory.buildRoleCoordinator();
        UserRol userRol = TestDataFactory.buildUserRol(adminUser, coordRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        assertThat(userRolService.hasAdminPrivileges(adminUser)).isTrue();
    }

    @Test
    @DisplayName("hasAdminPrivileges - retorna false para usuario con rol PROFESOR")
    void shouldReturnFalseForTeacher() {
        Role teacherRole = TestDataFactory.buildRoleProfesor();
        UserRol userRol = TestDataFactory.buildUserRol(teacherUser, teacherRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(teacherUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        assertThat(userRolService.hasAdminPrivileges(teacherUser)).isFalse();
    }

    @Test
    @DisplayName("hasTeacherPrivileges - retorna true para usuario con rol PROFESOR")
    void shouldReturnTrueForTeacherPrivileges() {
        Role teacherRole = TestDataFactory.buildRoleProfesor();
        UserRol userRol = TestDataFactory.buildUserRol(teacherUser, teacherRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(teacherUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        assertThat(userRolService.hasTeacherPrivileges(teacherUser)).isTrue();
    }

    @Test
    @DisplayName("hasTeacherPrivileges - retorna false para usuario con rol DIRECTOR")
    void shouldReturnFalseForAdminInTeacherCheck() {
        Role adminRole = TestDataFactory.buildRoleAdmin();
        UserRol userRol = TestDataFactory.buildUserRol(adminUser, adminRole, program);

        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        assertThat(userRolService.hasTeacherPrivileges(adminUser)).isFalse();
    }
}
