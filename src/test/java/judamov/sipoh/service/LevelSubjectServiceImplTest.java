package judamov.sipoh.service;

import judamov.sipoh.dto.LevelSubjectDTO;
import judamov.sipoh.entity.LevelSubject;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.ILevelSubjectRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.impl.LevelSubjectServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevelSubjectServiceImpl - Tests unitarios")
class LevelSubjectServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private ILevelSubjectRepository levelSubjectRepository;

    @InjectMocks
    private LevelSubjectServiceImpl levelSubjectService;

    private User adminUser;
    private User teacherUser;
    private LevelSubject levelSubject;

    @BeforeEach
    void setUp() {
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        levelSubject = TestDataFactory.buildLevelSubject();
    }

    @Test
    @DisplayName("getAll - retorna lista de niveles cuando el usuario es admin")
    void shouldReturnAllLevelsForAdmin() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(levelSubjectRepository.findAll()).thenReturn(List.of(levelSubject));

        List<LevelSubjectDTO> result = levelSubjectService.getAll(adminUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("PREGRADO");
    }

    @Test
    @DisplayName("getAll - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenWhenNotAdmin() {
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> levelSubjectService.getAll(teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("getAll - retorna lista vacía cuando no hay niveles")
    void shouldReturnEmptyListWhenNoLevels() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(levelSubjectRepository.findAll()).thenReturn(List.of());

        List<LevelSubjectDTO> result = levelSubjectService.getAll(adminUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserById - lanza NOT_FOUND cuando el usuario no existe")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levelSubjectService.getUserById(99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
