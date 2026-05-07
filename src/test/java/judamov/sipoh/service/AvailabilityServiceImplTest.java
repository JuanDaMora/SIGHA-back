package judamov.sipoh.service;

import judamov.sipoh.dto.AvailabilityBlockDTO;
import judamov.sipoh.dto.AvailabilityDTO;
import judamov.sipoh.dto.GlobalAvabilityDTO;
import judamov.sipoh.entity.*;
import judamov.sipoh.enums.DayOfWeekEnum;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.impl.AvailabilityServiceImpl;
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

import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityServiceImpl - Tests unitarios")
class AvailabilityServiceImplTest {

    @Mock private IAvailabilityRepository availabilityRepository;
    @Mock private IUserRoleRepository userRoleRepository;
    @Mock private IUserRepository userRepository;
    @Mock private ISemesterRepository semesterRepository;
    @Mock private IStatusAvailabilityRepository statusAvailabilityRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private IUserAreaRepository userAreaRepository;
    @Mock private ISubjectRepository subjectRepository;
    @Mock private IProgramRepository programRepository;

    @InjectMocks
    private AvailabilityServiceImpl availabilityService;

    private User adminUser;
    private User teacherUser;
    private Semester semester;
    private StatusAvailability statusPending;
    private StatusAvailability statusApproved;
    private Program program;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(availabilityService, "currentSchema", "ing_sistemas");
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        semester = TestDataFactory.buildSemester();
        statusPending = TestDataFactory.buildStatusPending();
        statusApproved = TestDataFactory.buildStatusApproved();
        program = TestDataFactory.buildProgram();
    }

    // ─── getUserById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById - retorna usuario cuando existe")
    void shouldReturnUserWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        User result = availabilityService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserById - lanza NOT_FOUND cuando el usuario no existe")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getUserById(99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ─── getSemesterById ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getSemesterById - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowWhenSemesterNotFound() {
        when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getSemesterById(99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }

    // ─── getAvailabilityByIdDocent ────────────────────────────────────────────

    @Test
    @DisplayName("getAvailabilityByIdDocent - retorna DTO vacío cuando el docente no tiene disponibilidad")
    void shouldReturnEmptyAvailabilityWhenNone() {
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(availabilityRepository.findByUserAndSemester(teacherUser, semester))
                .thenReturn(Optional.empty());

        AvailabilityDTO result = availabilityService.getAvailabilityByIdDocent(teacherUser.getId(), semester.getId());

        assertThat(result.getDisponibilidad()).isEmpty();
    }

    @Test
    @DisplayName("getAvailabilityByIdDocent - retorna bloques de disponibilidad correctamente mapeados")
    void shouldReturnMappedAvailabilityBlocks() {
        Availability availability = TestDataFactory.buildAvailability(teacherUser, semester, statusPending);

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(availabilityRepository.findByUserAndSemester(teacherUser, semester))
                .thenReturn(Optional.of(List.of(availability)));

        AvailabilityDTO result = availabilityService.getAvailabilityByIdDocent(teacherUser.getId(), semester.getId());

        assertThat(result.getDisponibilidad()).containsKey(DayOfWeekEnum.LUNES);
        List<AvailabilityBlockDTO> mondayBlocks = result.getDisponibilidad().get(DayOfWeekEnum.LUNES);
        assertThat(mondayBlocks).hasSize(1);
        assertThat(mondayBlocks.get(0).getHour()).isEqualTo(8);
    }

    // ─── getListGlobalAvailability ────────────────────────────────────────────

    @Test
    @DisplayName("getListGlobalAvailability - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenForNonAdminOnGlobalList() {
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> availabilityService.getListGlobalAvailability(semester.getId(), teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("getListGlobalAvailability - retorna lista de disponibilidad global agrupada por docente")
    void shouldReturnGlobalAvailabilityGroupedByTeacher() {
        Availability availability = TestDataFactory.buildAvailability(teacherUser, semester, statusPending);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(availabilityRepository.findBySemester(semester)).thenReturn(Optional.of(List.of(availability)));
        when(userAreaRepository.findByUserId(teacherUser.getId())).thenReturn(List.of());

        List<GlobalAvabilityDTO> result = availabilityService.getListGlobalAvailability(semester.getId(), adminUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserIdDocent()).isEqualTo(teacherUser.getId());
    }

    // ─── updateAvailabilityStatus ─────────────────────────────────────────────

    @Test
    @DisplayName("updateAvailabilityStatus - actualiza el estado de disponibilidad correctamente")
    void shouldUpdateAvailabilityStatusSuccessfully() {
        Availability availability = TestDataFactory.buildAvailability(teacherUser, semester, statusPending);

        when(availabilityRepository.findById(availability.getId())).thenReturn(Optional.of(availability));
        when(statusAvailabilityRepository.findById(statusApproved.getId())).thenReturn(Optional.of(statusApproved));

        Boolean result = availabilityService.updateAvailabilityStatus(availability.getId(), statusApproved.getId());

        assertThat(result).isTrue();
        assertThat(availability.getStatusAvailability()).isEqualTo(statusApproved);
        verify(availabilityRepository).save(availability);
    }

    @Test
    @DisplayName("updateAvailabilityStatus - lanza NOT_FOUND cuando la disponibilidad no existe")
    void shouldThrowWhenAvailabilityNotFound() {
        when(availabilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.updateAvailabilityStatus(99L, 2L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Disponibilidad no encontrada");
    }

    @Test
    @DisplayName("updateAvailabilityStatus - lanza NOT_FOUND cuando el nuevo estado no existe")
    void shouldThrowWhenNewStatusNotFound() {
        Availability availability = TestDataFactory.buildAvailability(teacherUser, semester, statusPending);

        when(availabilityRepository.findById(availability.getId())).thenReturn(Optional.of(availability));
        when(statusAvailabilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.updateAvailabilityStatus(availability.getId(), 99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Estado de disponibilidad no encontrado");
    }

    // ─── buildIncomingAvailabilityMap ─────────────────────────────────────────

    @Test
    @DisplayName("buildIncomingAvailabilityMap - construye el mapa día-hora correctamente")
    void shouldBuildIncomingMapCorrectly() {
        AvailabilityDTO dto = TestDataFactory.buildAvailabilityDTO(statusPending);

        Map<String, AvailabilityBlockDTO> result = availabilityService.buildIncomingAvailabilityMap(dto);

        assertThat(result).containsKey("LUNES-8");
    }

    // ─── getDefaultStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getDefaultStatus - lanza NOT_FOUND cuando no existe estado con id=1")
    void shouldThrowWhenDefaultStatusNotFound() {
        when(statusAvailabilityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getDefaultStatus())
                .isInstanceOf(GenericAppException.class);
    }

    // ─── deleteObsoleteAvailability ───────────────────────────────────────────

    @Test
    @DisplayName("deleteObsoleteAvailability - elimina bloques que no están en el nuevo mapa (usuario no admin)")
    void shouldDeleteObsoleteAvailabilityForNonAdmin() {
        Availability existing = TestDataFactory.buildAvailability(teacherUser, semester, statusPending);
        // mapa vacío → no hay bloques nuevos, el existente debe borrarse
        Map<String, AvailabilityBlockDTO> incomingMap = new HashMap<>();

        Role teacherRole = TestDataFactory.buildRoleProfesor();
        UserRol userRol = TestDataFactory.buildUserRol(teacherUser, teacherRole, program);

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(teacherUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        availabilityService.deleteObsoleteAvailability(List.of(existing), incomingMap, teacherUser.getId());

        verify(availabilityRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteObsoleteAvailability - no elimina bloques APROBADOS cuando el usuario es docente")
    void shouldNotDeleteApprovedBlocksForTeacher() {
        Availability approvedAvailability = TestDataFactory.buildAvailability(teacherUser, semester, statusApproved);
        Map<String, AvailabilityBlockDTO> incomingMap = new HashMap<>();

        Role teacherRole = TestDataFactory.buildRoleProfesor();
        UserRol userRol = TestDataFactory.buildUserRol(teacherUser, teacherRole, program);

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(teacherUser, program))
                .thenReturn(Optional.of(List.of(userRol)));

        availabilityService.deleteObsoleteAvailability(List.of(approvedAvailability), incomingMap, teacherUser.getId());

        verify(availabilityRepository, never()).delete(approvedAvailability);
    }

    // ─── saveNewAvailabilityBlocks ────────────────────────────────────────────

    @Test
    @DisplayName("saveNewAvailabilityBlocks - no guarda un bloque que ya existe")
    void shouldSkipAlreadyExistingBlocks() {
        AvailabilityDTO dto = TestDataFactory.buildAvailabilityDTO(statusPending);

        when(availabilityRepository.existsByUserAndSemesterAndDayOfWeekAndStartTime(
                eq(teacherUser), eq(semester), eq(DayOfWeekEnum.LUNES), eq(LocalTime.of(8, 0))))
                .thenReturn(true);

        availabilityService.saveNewAvailabilityBlocks(dto, teacherUser, semester, new HashMap<>(), statusPending);

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveNewAvailabilityBlocks - guarda un bloque nuevo correctamente")
    void shouldSaveNewBlock() {
        AvailabilityDTO dto = TestDataFactory.buildAvailabilityDTO(statusPending);

        when(availabilityRepository.existsByUserAndSemesterAndDayOfWeekAndStartTime(
                any(), any(), any(), any())).thenReturn(false);
        when(statusAvailabilityRepository.findById(statusPending.getId()))
                .thenReturn(Optional.of(statusPending));

        availabilityService.saveNewAvailabilityBlocks(dto, teacherUser, semester, new HashMap<>(), statusPending);

        verify(availabilityRepository).save(any(Availability.class));
    }
}
