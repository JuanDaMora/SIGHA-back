package judamov.sipoh.service;

import judamov.sipoh.dto.SubjectCreateDTO;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.impl.SubjectServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectServiceImpl - Tests unitarios")
class SubjectServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private ISubjectRepository subjectRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private IAreaRepository areaRepository;
    @Mock private ILevelSubjectRepository levelSubjectRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private User adminUser;
    private User teacherUser;
    private Area area;
    private LevelSubject levelSubject;

    @BeforeEach
    void setUp() {
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        area = TestDataFactory.buildArea();
        levelSubject = TestDataFactory.buildLevelSubject();
    }

    private SubjectCreateDTO buildSubjectCreateDTO() {
        SubjectCreateDTO dto = new SubjectCreateDTO();
        dto.setCode("MAT001");
        dto.setName("Cálculo I");
        dto.setIdArea(area.getId());
        dto.setIdLevel(levelSubject.getId());
        return dto;
    }

    // ─── createSubject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSubject - crea asignatura exitosamente con datos válidos")
    void shouldCreateSubjectSuccessfully() {
        SubjectCreateDTO dto = buildSubjectCreateDTO();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(levelSubjectRepository.findById(levelSubject.getId())).thenReturn(Optional.of(levelSubject));

        Boolean result = subjectService.createSubject(dto, adminUser.getId());

        assertThat(result).isTrue();
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    @DisplayName("createSubject - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenWhenNotAdmin() {
        SubjectCreateDTO dto = buildSubjectCreateDTO();

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.createSubject(dto, teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("createSubject - lanza NOT_FOUND cuando el área no existe")
    void shouldThrowWhenAreaNotFound() {
        SubjectCreateDTO dto = buildSubjectCreateDTO();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(areaRepository.findById(area.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.createSubject(dto, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Área no encontrada");
    }

    @Test
    @DisplayName("createSubject - lanza NOT_FOUND cuando el nivel académico no existe")
    void shouldThrowWhenLevelNotFound() {
        SubjectCreateDTO dto = buildSubjectCreateDTO();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(levelSubjectRepository.findById(levelSubject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.createSubject(dto, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Nivel académico no encontrado");
    }

    // ─── updateSubject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateSubject - actualiza asignatura correctamente")
    void shouldUpdateSubjectSuccessfully() {
        SubjectCreateDTO dto = buildSubjectCreateDTO();
        Subject existing = TestDataFactory.buildSubject(area, levelSubject);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(areaRepository.findById(area.getId())).thenReturn(Optional.of(area));
        when(levelSubjectRepository.findById(levelSubject.getId())).thenReturn(Optional.of(levelSubject));

        Boolean result = subjectService.updateSubject(existing.getId(), dto, adminUser.getId());

        assertThat(result).isTrue();
        verify(subjectRepository).save(existing);
    }

    @Test
    @DisplayName("updateSubject - lanza NOT_FOUND cuando la asignatura no existe")
    void shouldThrowWhenSubjectNotFound() {
        SubjectCreateDTO dto = buildSubjectCreateDTO();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.updateSubject(99L, dto, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Asignatura no encontrada");
    }
}
