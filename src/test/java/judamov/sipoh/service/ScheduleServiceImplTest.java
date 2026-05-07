package judamov.sipoh.service;

import judamov.sipoh.dto.ScheduleCreateDTO;
import judamov.sipoh.dto.ScheduleDTO;
import judamov.sipoh.entity.*;
import judamov.sipoh.enums.DayOfWeekEnum;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IGroupRepository;
import judamov.sipoh.repository.IScheduleRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.impl.ScheduleServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.service.interfaces.IGroupService;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleServiceImpl - Tests unitarios")
class ScheduleServiceImplTest {

    @Mock private IScheduleRepository scheduleRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private IUserRepository userRepository;
    @Mock private IGroupRepository groupRepository;
    @Mock private IGroupService groupService;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private User adminUser;
    private User teacherUser;
    private Group group;
    private Semester semester;

    @BeforeEach
    void setUp() {
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        semester = TestDataFactory.buildSemester();

        Area area = TestDataFactory.buildArea();
        LevelSubject level = TestDataFactory.buildLevelSubject();
        Subject subject = TestDataFactory.buildSubject(area, level);
        group = TestDataFactory.buildGroup(subject, semester, teacherUser);
    }

    // ─── createSchedule ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createSchedule - crea horarios correctamente borrando los anteriores")
    void shouldCreateSchedulesSuccessfully() {
        ScheduleDTO scheduleDTO = TestDataFactory.buildScheduleDTO();
        ScheduleCreateDTO dto = new ScheduleCreateDTO(group.getId(), teacherUser.getId(), List.of(scheduleDTO));

        Schedule savedSchedule = new Schedule();
        savedSchedule.setId(10L);
        savedSchedule.setGroup(group);
        savedSchedule.setDay(DayOfWeekEnum.LUNES);
        savedSchedule.setStartTime(LocalTime.of(8, 0));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of()));
        when(scheduleRepository.saveAll(anyList())).thenReturn(List.of(savedSchedule));
        when(groupService.updateDocente(group.getId(), teacherUser.getId(), adminUser.getId())).thenReturn(true);

        List<ScheduleDTO> result = scheduleService.createSchedule(dto, adminUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHour()).isEqualTo(8);
        assertThat(result.get(0).getDay()).isEqualTo(DayOfWeekEnum.LUNES);
    }

    @Test
    @DisplayName("createSchedule - elimina horarios previos antes de crear los nuevos")
    void shouldDeletePreviousSchedulesBeforeCreatingNew() {
        ScheduleDTO scheduleDTO = TestDataFactory.buildScheduleDTO();
        ScheduleCreateDTO dto = new ScheduleCreateDTO(group.getId(), teacherUser.getId(), List.of(scheduleDTO));

        Schedule existingSchedule = new Schedule();
        existingSchedule.setId(5L);
        existingSchedule.setGroup(group);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of(existingSchedule)));
        when(scheduleRepository.saveAll(anyList())).thenReturn(List.of());
        when(groupService.updateDocente(any(), any(), any())).thenReturn(true);

        scheduleService.createSchedule(dto, adminUser.getId());

        verify(scheduleRepository).deleteAll(List.of(existingSchedule));
    }

    @Test
    @DisplayName("createSchedule - lanza NOT_FOUND cuando el grupo no existe")
    void shouldThrowWhenGroupNotFound() {
        ScheduleCreateDTO dto = new ScheduleCreateDTO(99L, teacherUser.getId(), List.of());

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.createSchedule(dto, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Grupo no encontrado");
    }

    @Test
    @DisplayName("createSchedule - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenForNonAdmin() {
        ScheduleCreateDTO dto = new ScheduleCreateDTO(group.getId(), null, List.of());

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.createSchedule(dto, teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    // ─── deleteSceduleByGroup ─────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSceduleByGroup - elimina los horarios del grupo si existen")
    void shouldDeleteSchedulesByGroup() {
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setGroup(group);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of(schedule)));

        scheduleService.deleteSceduleByGroup(group, adminUser.getId());

        verify(scheduleRepository).deleteAll(List.of(schedule));
    }

    @Test
    @DisplayName("deleteSceduleByGroup - no falla si el grupo no tiene horarios")
    void shouldNotFailWhenGroupHasNoSchedules() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.empty());

        scheduleService.deleteSceduleByGroup(group, adminUser.getId());

        verify(scheduleRepository, never()).deleteAll(anyList());
    }
}
