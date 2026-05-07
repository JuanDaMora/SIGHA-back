package judamov.sipoh.service;

import judamov.sipoh.dto.SemesterDTO;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.ISemesterRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.impl.SemesterServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemesterServiceImpl - Tests unitarios")
class SemesterServiceImplTest {

    @Mock private ISemesterRepository semesterRepository;
    @Mock private IUserRepository userRepository;
    @Mock private UserRolServiceImpl userRolService;

    @InjectMocks
    private SemesterServiceImpl semesterService;

    private User adminUser;
    private User teacherUser;
    private Semester semester;

    @BeforeEach
    void setUp() {
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        semester = TestDataFactory.buildSemester();
    }

    @Test
    @DisplayName("getAllSemesters - retorna lista ordenada por id ascendente")
    void shouldReturnAllSemestersOrdered() {
        when(semesterRepository.findAll(any(Sort.class))).thenReturn(List.of(semester));

        List<Semester> result = semesterService.getAllSemesters();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("2025-1");
    }

    @Test
    @DisplayName("addSemester - guarda un semestre nuevo exitosamente")
    void shouldAddSemesterSuccessfully() {
        SemesterDTO dto = TestDataFactory.buildSemesterDTO();
        when(semesterRepository.findOneByDescription(dto.getDescription())).thenReturn(Optional.empty());

        Boolean result = semesterService.addSemester(dto);

        assertThat(result).isTrue();
        verify(semesterRepository).save(any(Semester.class));
    }

    @Test
    @DisplayName("addSemester - lanza CONFLICT cuando ya existe un semestre con la misma descripción")
    void shouldThrowConflictWhenDescriptionAlreadyExists() {
        SemesterDTO dto = TestDataFactory.buildSemesterDTO();
        when(semesterRepository.findOneByDescription(dto.getDescription())).thenReturn(Optional.of(semester));

        assertThatThrownBy(() -> semesterService.addSemester(dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Ya existe un semestre con la descripción");
    }

    @Test
    @DisplayName("updateSemester - actualiza los datos del semestre y retorna DTO actualizado")
    void shouldUpdateSemesterSuccessfully() {
        SemesterDTO dto = TestDataFactory.buildSemesterDTO();
        dto.setId(semester.getId());

        when(semesterRepository.findOneById(semester.getId())).thenReturn(Optional.of(semester));
        when(semesterRepository.save(semester)).thenReturn(semester);

        SemesterDTO result = semesterService.updateSemester(dto);

        assertThat(result).isNotNull();
        verify(semesterRepository).save(semester);
    }

    @Test
    @DisplayName("updateSemester - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowNotFoundWhenSemesterDoesNotExist() {
        SemesterDTO dto = TestDataFactory.buildSemesterDTO();
        dto.setId(99L);

        when(semesterRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.updateSemester(dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }

    @Test
    @DisplayName("changeAvailability - cambia el estado de disponibilidad del semestre")
    void shouldChangeAvailabilitySuccessfully() {
        semester.setAvailability(true);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findOneById(semester.getId())).thenReturn(Optional.of(semester));

        Boolean result = semesterService.changeAvailability(false, semester.getId(), adminUser.getId());

        assertThat(result).isTrue();
        assertThat(semester.getAvailability()).isFalse();
        verify(semesterRepository).save(semester);
    }

    @Test
    @DisplayName("changeAvailability - no llama a save si el estado ya es el mismo")
    void shouldNotSaveWhenAvailabilityIsAlreadySame() {
        semester.setAvailability(true);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findOneById(semester.getId())).thenReturn(Optional.of(semester));

        semesterService.changeAvailability(true, semester.getId(), adminUser.getId());

        verify(semesterRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeAvailability - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenWhenNotAdmin() {
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> semesterService.changeAvailability(false, semester.getId(), teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("changeAvailability - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowNotFoundWhenSemesterNotFound() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.changeAvailability(false, 99L, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }
}
