package judamov.sipoh.service;

import judamov.sipoh.dto.IndividualAvailabilityDTO;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IIndividualAvailabilityRepository;
import judamov.sipoh.repository.ISemesterRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.impl.IndividualAvailabilityImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndividualAvailabilityImpl - Tests unitarios")
class IndividualAvailabilityImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private ISemesterRepository semesterRepository;
    @Mock private IIndividualAvailabilityRepository individualAvailabilityRepository;

    @InjectMocks
    private IndividualAvailabilityImpl individualAvailabilityService;

    private User adminUser;
    private User teacherUser;
    private Semester semester;

    @BeforeEach
    void setUp() {
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        semester = TestDataFactory.buildSemester();
    }

    // ─── upsertIndividualAvailability ─────────────────────────────────────────

    @Test
    @DisplayName("upsertIndividualAvailability - crea nueva disponibilidad cuando no existe")
    void shouldCreateAvailabilityWhenNotExists() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasTeacherPrivileges(teacherUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(individualAvailabilityRepository.findBySemesterAndUser(semester, teacherUser))
                .thenReturn(List.of());
        when(individualAvailabilityRepository.save(any(IndividualAvailability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IndividualAvailabilityDTO result = individualAvailabilityService
                .upsertIndividualAvailability(semester.getId(), adminUser.getId(), true, teacherUser.getId());

        assertThat(result.getUserId()).isEqualTo(teacherUser.getId());
        assertThat(result.getSemesterId()).isEqualTo(semester.getId());
        assertThat(result.getIsActive()).isTrue();
        verify(individualAvailabilityRepository).save(any(IndividualAvailability.class));
    }

    @Test
    @DisplayName("upsertIndividualAvailability - actualiza estado cuando ya existe y el estado difiere")
    void shouldUpdateAvailabilityWhenStateChanges() {
        IndividualAvailability existing = TestDataFactory.buildIndividualAvailability(teacherUser, semester);
        existing.setIsActive(false);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasTeacherPrivileges(teacherUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(individualAvailabilityRepository.findBySemesterAndUser(semester, teacherUser))
                .thenReturn(List.of(existing));
        when(individualAvailabilityRepository.save(any(IndividualAvailability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IndividualAvailabilityDTO result = individualAvailabilityService
                .upsertIndividualAvailability(semester.getId(), adminUser.getId(), true, teacherUser.getId());

        assertThat(result.getIsActive()).isTrue();
        verify(individualAvailabilityRepository).save(existing);
    }

    @Test
    @DisplayName("upsertIndividualAvailability - no actualiza cuando el estado ya es el mismo")
    void shouldNotUpdateWhenStateIsTheSame() {
        IndividualAvailability existing = TestDataFactory.buildIndividualAvailability(teacherUser, semester);
        existing.setIsActive(true);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasTeacherPrivileges(teacherUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(individualAvailabilityRepository.findBySemesterAndUser(semester, teacherUser))
                .thenReturn(List.of(existing));
        when(individualAvailabilityRepository.save(any(IndividualAvailability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        individualAvailabilityService
                .upsertIndividualAvailability(semester.getId(), adminUser.getId(), true, teacherUser.getId());

        assertThat(existing.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("upsertIndividualAvailability - lanza FORBIDDEN cuando el que actúa no es admin")
    void shouldThrowForbiddenWhenActorIsNotAdmin() {
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> individualAvailabilityService
                .upsertIndividualAvailability(semester.getId(), teacherUser.getId(), true, teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("upsertIndividualAvailability - lanza FORBIDDEN cuando el docenteId no tiene rol PROFESOR")
    void shouldThrowForbiddenWhenDocenteIsNotTeacher() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasTeacherPrivileges(adminUser)).thenReturn(false);

        assertThatThrownBy(() -> individualAvailabilityService
                .upsertIndividualAvailability(semester.getId(), adminUser.getId(), true, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("no tiene rol de docente");
    }

    @Test
    @DisplayName("upsertIndividualAvailability - lanza INTERNAL_SERVER_ERROR si hay múltiples registros duplicados")
    void shouldThrowWhenDuplicateRecordsFound() {
        IndividualAvailability dup1 = TestDataFactory.buildIndividualAvailability(teacherUser, semester);
        IndividualAvailability dup2 = TestDataFactory.buildIndividualAvailability(teacherUser, semester);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasTeacherPrivileges(teacherUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(individualAvailabilityRepository.findBySemesterAndUser(semester, teacherUser))
                .thenReturn(List.of(dup1, dup2));

        assertThatThrownBy(() -> individualAvailabilityService
                .upsertIndividualAvailability(semester.getId(), adminUser.getId(), true, teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("consistencia");
    }

    // ─── getStatusIndividualAvailability ─────────────────────────────────────

    @Test
    @DisplayName("getStatusIndividualAvailability - retorna el DTO cuando existe disponibilidad")
    void shouldReturnAvailabilityWhenFound() {
        IndividualAvailability existing = TestDataFactory.buildIndividualAvailability(teacherUser, semester);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(individualAvailabilityRepository.findBySemesterAndUser(semester, teacherUser))
                .thenReturn(List.of(existing));

        IndividualAvailabilityDTO result = individualAvailabilityService
                .getStatusIndividualAvailability(semester.getId(), adminUser.getId(), teacherUser.getId());

        assertThat(result.getUserId()).isEqualTo(teacherUser.getId());
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("getStatusIndividualAvailability - lanza NOT_FOUND cuando no existe registro")
    void shouldThrowWhenAvailabilityNotFound() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(individualAvailabilityRepository.findBySemesterAndUser(semester, teacherUser))
                .thenReturn(List.of());

        assertThatThrownBy(() -> individualAvailabilityService
                .getStatusIndividualAvailability(semester.getId(), adminUser.getId(), teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("No existe disponibilidad individual");
    }
}
