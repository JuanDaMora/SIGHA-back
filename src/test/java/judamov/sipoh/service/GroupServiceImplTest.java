package judamov.sipoh.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.impl.GroupServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.service.interfaces.IScheduleService;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupServiceImpl - Tests unitarios")
class GroupServiceImplTest {

    @Mock private IGroupRepository groupRepository;
    @Mock private IUserRepository userRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private ISubjectRepository subjectRepository;
    @Mock private ISemesterRepository semesterRepository;
    @Mock private IScheduleRepository scheduleRepository;
    @Mock private IScheduleService scheduleService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private GroupServiceImpl groupService;

    private User adminUser;
    private User teacherUser;
    private Semester semester;
    private Subject subject;
    private Group group;

    @BeforeEach
    void setUp() {
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        semester = TestDataFactory.buildSemester();
        Area area = TestDataFactory.buildArea();
        LevelSubject level = TestDataFactory.buildLevelSubject();
        subject = TestDataFactory.buildSubject(area, level);
        group = TestDataFactory.buildGroup(subject, semester, teacherUser);
    }

    // ─── getAllBySemester ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllBySemester - retorna grupos del semestre para un admin")
    void shouldReturnGroupsBySemesterForAdmin() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findAll()).thenReturn(List.of(group));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of()));

        List<GroupDTO> result = groupService.getAllBySemester(adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("G01");
    }

    @Test
    @DisplayName("getAllBySemester - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenOnGetAllBySemester() {
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> groupService.getAllBySemester(teacherUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    // ─── createGroup ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createGroup - crea grupo exitosamente con datos válidos")
    void shouldCreateGroupSuccessfully() {
        ScheduleDTO scheduleDTO = TestDataFactory.buildScheduleDTO();
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setCode("G02");
        dto.setIdSubject(subject.getId());
        dto.setIdDocente(teacherUser.getId());
        dto.setMax_students("30");
        dto.setEnrolled("0");
        dto.setScheduleList(List.of(scheduleDTO));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(scheduleRepository.existsByGroupDocenteAndGroupSemesterAndDayAndStartTime(any(), any(), any(), any()))
                .thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(scheduleService.createSchedule(any(ScheduleCreateDTO.class), eq(adminUser.getId())))
                .thenReturn(List.of(scheduleDTO));

        Boolean result = groupService.createGroup(dto, adminUser.getId(), semester.getId());

        assertThat(result).isTrue();
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    @DisplayName("createGroup - lanza FORBIDDEN cuando el usuario no es admin")
    void shouldThrowForbiddenOnCreateGroup() {
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setIdSubject(subject.getId());

        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> groupService.createGroup(dto, teacherUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("createGroup - lanza NOT_FOUND cuando la materia no existe")
    void shouldThrowWhenSubjectNotFound() {
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setIdSubject(99L);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(dto, adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Materia no encontrada");
    }

    // ─── deleteGroup ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteGroup - elimina grupo y sus horarios exitosamente")
    void shouldDeleteGroupSuccessfully() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        doNothing().when(scheduleService).deleteSceduleByGroup(group, adminUser.getId());

        Boolean result = groupService.deleteGroup(group.getId(), adminUser.getId());

        assertThat(result).isTrue();
        verify(groupRepository).delete(group);
        verify(scheduleService).deleteSceduleByGroup(group, adminUser.getId());
    }

    @Test
    @DisplayName("deleteGroup - lanza NOT_FOUND cuando el grupo no existe")
    void shouldThrowWhenGroupNotFoundOnDelete() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.deleteGroup(99L, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Grupo no encontrado");
    }

    // ─── updateDocente ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateDocente - actualiza el docente del grupo correctamente")
    void shouldUpdateDocenteSuccessfully() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));

        Boolean result = groupService.updateDocente(group.getId(), teacherUser.getId(), adminUser.getId());

        assertThat(result).isTrue();
        verify(groupRepository).save(group);
        assertThat(group.getDocente()).isEqualTo(teacherUser);
    }

    @Test
    @DisplayName("updateDocente - asigna docente null cuando idDocente es null")
    void shouldSetDocenteNullWhenIdDocenteIsNull() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        Boolean result = groupService.updateDocente(group.getId(), null, adminUser.getId());

        assertThat(result).isTrue();
        assertThat(group.getDocente()).isNull();
    }

    // ─── getAllByFilters ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllByFilters - retorna todos los grupos si no hay filtros")
    void shouldReturnAllGroupsWhenNoFilters() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findBySemester(semester)).thenReturn(List.of(group));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of()));

        List<GroupDTO> result = groupService.getAllByFilters(null, null, null, adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getAllByDocente - retorna grupos del docente en el semestre")
    void shouldReturnGroupsByDocente() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findByDocenteAndSemester(teacherUser, semester))
                .thenReturn(Optional.of(List.of(group)));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of()));

        List<GroupDTO> result = groupService.getAllByDocente(teacherUser.getId(), adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getAllByDocente - lanza NOT_FOUND cuando el docente no tiene grupos en el semestre")
    void shouldThrowNotFoundWhenDocenteHasNoGroups() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findByDocenteAndSemester(teacherUser, semester)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getAllByDocente(teacherUser.getId(), adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("no tiene grupos");
    }

    @Test
    @DisplayName("getAllBySubject - lanza NOT_FOUND cuando la materia no existe")
    void shouldThrowNotFoundWhenSubjectMissingForGetBySubject() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getAllBySubject(99L, adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Materia no encontrada");
    }

    @Test
    @DisplayName("getAllBySubject - retorna grupos de la materia en el semestre")
    void shouldReturnGroupsBySubject() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findBySubjectAndSemester(subject, semester))
                .thenReturn(Optional.of(List.of(group)));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of()));

        List<GroupDTO> result = groupService.getAllBySubject(subject.getId(), adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("createGroup - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowNotFoundWhenSemesterMissingOnCreateGroup() {
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setIdSubject(subject.getId());
        dto.setCode("G99");

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(dto, adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }

    @Test
    @DisplayName("createGroup - lanza CONFLICT cuando el docente ya tiene otro grupo en el mismo bloque")
    void shouldThrowConflictWhenDocenteScheduleOverlaps() {
        ScheduleDTO scheduleDTO = TestDataFactory.buildScheduleDTO();
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setCode("G02");
        dto.setIdSubject(subject.getId());
        dto.setIdDocente(teacherUser.getId());
        dto.setMax_students("30");
        dto.setEnrolled("0");
        dto.setScheduleList(List.of(scheduleDTO));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(scheduleRepository.existsByGroupDocenteAndGroupSemesterAndDayAndStartTime(
                teacherUser, semester, scheduleDTO.getDay(), java.time.LocalTime.of(scheduleDTO.getHour(), 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(dto, adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .matches(ex -> ((GenericAppException) ex).getStatus().isSameCodeAs(
                        org.springframework.http.HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("getAllByLevels - retorna grupos filtrados por nivel")
    void shouldReturnGroupsByLevels() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findBySemester(semester)).thenReturn(List.of(group));
        when(scheduleRepository.findByGroup(group)).thenReturn(Optional.of(List.of()));

        List<GroupDTO> result = groupService.getAllByLevels(
                List.of(subject.getLevelSubject().getId()), adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("deleteAllGroupsBySemesterId - elimina grupos y horarios del semestre")
    void shouldDeleteAllGroupsBySemester() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findBySemester(semester)).thenReturn(List.of(group));
        doNothing().when(scheduleRepository).deleteByGroupIn(anyList());
        doNothing().when(groupRepository).deleteAll(anyList());

        Boolean result = groupService.deleteAllGroupsBySemesterId(adminUser.getId(), semester.getId());

        assertThat(result).isTrue();
        verify(scheduleRepository).deleteByGroupIn(List.of(group));
        verify(groupRepository).deleteAll(List.of(group));
    }

    @Test
    @DisplayName("deleteAllGroupsBySemesterId - lanza NOT_FOUND cuando no hay grupos en el semestre")
    void shouldThrowNotFoundWhenNoGroupsInSemesterForBulkDelete() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findBySemester(semester)).thenReturn(List.of());

        assertThatThrownBy(() -> groupService.deleteAllGroupsBySemesterId(adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("No se encontraron grupos");
    }

    @Test
    @DisplayName("updateGroup - actualiza datos del grupo sin horarios nuevos")
    void shouldUpdateGroupWithoutSchedules() {
        GroupUpdateDTO dto = new GroupUpdateDTO();
        dto.setCode("G03");
        dto.setIdSubject(subject.getId());
        dto.setIdDocente(null);
        dto.setMax_students("40");
        dto.setEnrolled("5");
        dto.setScheduleList(null);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));

        Boolean result = groupService.updateGroup(group.getId(), dto, adminUser.getId(), semester.getId());

        assertThat(result).isTrue();
        verify(groupRepository).save(group);
        verify(scheduleService, never()).createSchedule(any(), anyLong());
    }

    @Test
    @DisplayName("createGroupsBulk - delega en createGroup por cada DTO")
    void shouldCreateGroupsBulk() {
        ScheduleDTO scheduleDTO = TestDataFactory.buildScheduleDTO();
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setCode("G02");
        dto.setIdSubject(subject.getId());
        dto.setIdDocente(teacherUser.getId());
        dto.setMax_students("30");
        dto.setEnrolled("0");
        dto.setScheduleList(List.of(scheduleDTO));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(scheduleRepository.existsByGroupDocenteAndGroupSemesterAndDayAndStartTime(any(), any(), any(), any()))
                .thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(scheduleService.createSchedule(any(ScheduleCreateDTO.class), eq(adminUser.getId())))
                .thenReturn(List.of(scheduleDTO));

        Boolean result = groupService.createGroupsBulk(List.of(dto, dto), adminUser.getId(), semester.getId());

        assertThat(result).isTrue();
        verify(groupRepository, times(2)).save(any(Group.class));
    }

    private Object[] rowAllProgramsView() {
        return new Object[]{
                1L, 1L, 1L, "SUB", "Subject", 2L, 5L, "Nivel", "G1", "30", "10",
                1L, "ing_sistemas", "Prog", "Escuela"
        };
    }

    @Test
    @DisplayName("getAllBySemesterAllPrograms - filtra grupos sin horarios y carga nativos por schema")
    void shouldLoadSchedulesForAllProgramsSemester() {
        Query query = mock(Query.class);
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findAllProgramsBySemester(semester.getId()))
                .thenReturn(List.<Object[]>of(rowAllProgramsView()));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("groupIds"), anyList())).thenReturn(query);
        List<Object[]> scheduleRows = new ArrayList<>();
        scheduleRows.add(new Object[]{10L, 1L, 8, "LUNES"});
        when(query.getResultList()).thenReturn(scheduleRows);

        List<GroupDTO> result = groupService.getAllBySemesterAllPrograms(adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduleList()).isNotEmpty();
    }

    @Test
    @DisplayName("getAllByFiltersAllPrograms - sin filtros devuelve solo grupos con horarios cargados")
    void shouldReturnFilteredAllProgramsWhenNoExtraFilters() {
        Query query = mock(Query.class);
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findAllProgramsBySemester(semester.getId()))
                .thenReturn(List.<Object[]>of(rowAllProgramsView()));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("groupIds"), anyList())).thenReturn(query);
        when(query.getResultList()).thenReturn(new ArrayList<>());

        List<GroupDTO> result = groupService.getAllByFiltersAllPrograms(
                null, null, null, null, adminUser.getId(), semester.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllByFiltersAllPrograms - aplica filtro por nivel")
    void shouldFilterAllProgramsByLevel() {
        Query query = mock(Query.class);
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findAllProgramsBySemester(semester.getId()))
                .thenReturn(List.<Object[]>of(rowAllProgramsView()));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("groupIds"), anyList())).thenReturn(query);
        List<Object[]> scheduleRowsLevel = new ArrayList<>();
        scheduleRowsLevel.add(new Object[]{10L, 1L, 8, "LUNES"});
        when(query.getResultList()).thenReturn(scheduleRowsLevel);

        List<GroupDTO> result = groupService.getAllByFiltersAllPrograms(
                List.of(5L), null, null, null, adminUser.getId(), semester.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getAllByFilters - filtra por docente y materia")
    void shouldFilterGroupsByDocenteAndSubject() {
        Group g2 = new Group();
        g2.setId(2L);
        g2.setCode("G2");
        g2.setSemester(semester);
        g2.setSubject(subject);
        g2.setDocente(teacherUser);
        User other = TestDataFactory.buildAdminUser();
        other.setId(99L);
        Group g3 = new Group();
        g3.setId(3L);
        g3.setCode("G3");
        g3.setSemester(semester);
        g3.setSubject(subject);
        g3.setDocente(other);

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(groupRepository.findBySemester(semester)).thenReturn(List.of(group, g2, g3));
        when(scheduleRepository.findByGroup(any(Group.class))).thenReturn(Optional.of(List.of()));

        List<GroupDTO> result = groupService.getAllByFilters(
                null, List.of(teacherUser.getId()), List.of(subject.getId()), adminUser.getId(), semester.getId());

        assertThat(result).extracting(GroupDTO::getId).containsExactlyInAnyOrder(group.getId(), g2.getId());
    }

    @Test
    @DisplayName("createGroup - lanza NOT_FOUND cuando el docente no existe")
    void shouldThrowNotFoundWhenDocenteMissingOnCreateGroup() {
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setCode("G");
        dto.setIdSubject(subject.getId());
        dto.setIdDocente(99L);
        dto.setScheduleList(List.of());
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(dto, adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Docente no encontrado");
    }

    @Test
    @DisplayName("updateDocente - lanza NOT_FOUND cuando el nuevo docente no existe")
    void shouldThrowNotFoundWhenNewDocenteMissing() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.updateDocente(group.getId(), 99L, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Docente no encontrado");
    }

    @Test
    @DisplayName("updateGroup - lanza NOT_FOUND cuando el grupo no existe")
    void shouldThrowNotFoundWhenGroupMissingOnUpdate() {
        GroupUpdateDTO dto = new GroupUpdateDTO();
        dto.setIdSubject(subject.getId());
        dto.setCode("x");
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.updateGroup(99L, dto, adminUser.getId(), semester.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Grupo no encontrado");
    }

    @Test
    @DisplayName("updateGroup - con horarios llama a createSchedule y valida conflictos excluyendo el grupo")
    void shouldUpdateGroupWithSchedulesExcludingSelf() {
        ScheduleDTO scheduleDTO = TestDataFactory.buildScheduleDTO();
        GroupUpdateDTO dto = new GroupUpdateDTO();
        dto.setCode("G3");
        dto.setIdSubject(subject.getId());
        dto.setIdDocente(teacherUser.getId());
        dto.setMax_students("20");
        dto.setEnrolled("1");
        dto.setScheduleList(List.of(scheduleDTO));

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(semester.getId())).thenReturn(Optional.of(semester));
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(scheduleRepository.existsByGroupDocenteAndGroupSemesterAndDayAndStartTimeAndGroupIdNot(
                teacherUser, semester, scheduleDTO.getDay(), java.time.LocalTime.of(scheduleDTO.getHour(), 0), group.getId()))
                .thenReturn(false);
        when(scheduleService.createSchedule(any(ScheduleCreateDTO.class), eq(adminUser.getId())))
                .thenReturn(List.of(scheduleDTO));

        assertThat(groupService.updateGroup(group.getId(), dto, adminUser.getId(), semester.getId())).isTrue();
        verify(scheduleService).createSchedule(any(ScheduleCreateDTO.class), eq(adminUser.getId()));
    }

    @Test
    @DisplayName("getAllByLevels - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowNotFoundWhenSemesterMissingOnGetByLevels() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getAllByLevels(List.of(1L), adminUser.getId(), 99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }

    @Test
    @DisplayName("getAllBySubject - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowNotFoundWhenSemesterMissingOnGetBySubject() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getAllBySubject(subject.getId(), adminUser.getId(), 99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }

    @Test
    @DisplayName("getAllByDocente - lanza NOT_FOUND cuando el semestre no existe")
    void shouldThrowNotFoundWhenSemesterMissingOnGetByDocente() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getAllByDocente(teacherUser.getId(), adminUser.getId(), 99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Semestre no encontrado");
    }
}
