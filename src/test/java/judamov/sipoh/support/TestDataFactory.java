package judamov.sipoh.support;

import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.enums.DayOfWeekEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fábrica centralizada de objetos de prueba (entidades y DTOs).
 * Reutilizable en todos los tests unitarios para evitar duplicación de datos.
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    // ─── Programas ───────────────────────────────────────────────────────────

    public static Program buildProgram() {
        Program program = new Program();
        program.setId(1L);
        program.setName("Ingeniería de Sistemas");
        program.setCode("ing_sistemas");
        return program;
    }

    // ─── Roles ───────────────────────────────────────────────────────────────

    public static Role buildRoleAdmin() {
        Role role = new Role();
        role.setId(1L);
        role.setName("DIRECTOR DE ESCUELA");
        role.setUserRoles(new ArrayList<>());
        return role;
    }

    public static Role buildRoleCoordinator() {
        Role role = new Role();
        role.setId(2L);
        role.setName("COORDINADOR ACADEMICO");
        role.setUserRoles(new ArrayList<>());
        return role;
    }

    public static Role buildRoleProfesor() {
        Role role = new Role();
        role.setId(3L);
        role.setName("PROFESOR");
        role.setUserRoles(new ArrayList<>());
        return role;
    }

    // ─── Usuarios ────────────────────────────────────────────────────────────

    public static User buildAdminUser() {
        TypeDocument typeDoc = buildTypeDocument();
        typeDoc.setSigla(buildSigla());
        User user = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("Test")
                .documento("11111111")
                .email("admin@test.com")
                .password("encodedPass")
                .active(true)
                .typeDocument(typeDoc)
                .userRoles(new ArrayList<>())
                .userAreas(new ArrayList<>())
                .build();
        return user;
    }

    public static User buildTeacherUser() {
        TypeDocument typeDoc = buildTypeDocument();
        typeDoc.setSigla(buildSigla());
        User user = User.builder()
                .id(2L)
                .firstName("Docente")
                .lastName("Test")
                .documento("22222222")
                .email("docente@test.com")
                .password("encodedPass")
                .active(true)
                .typeDocument(typeDoc)
                .userRoles(new ArrayList<>())
                .userAreas(new ArrayList<>())
                .build();
        return user;
    }

    public static UserRol buildUserRol(User user, Role role, Program program) {
        return new UserRol(null, user, role, program, null, null);
    }

    // ─── Semestres ───────────────────────────────────────────────────────────

    public static Semester buildSemester() {
        Semester s = new Semester();
        s.setId(1L);
        s.setDescription("2025-1");
        s.setStartDate(LocalDate.of(2025, 1, 15));
        s.setEndDate(LocalDate.of(2025, 6, 30));
        s.setAvailability(true);
        return s;
    }

    // ─── Estados de disponibilidad ───────────────────────────────────────────

    public static StatusAvailability buildStatusPending() {
        return StatusAvailability.builder()
                .id(1L)
                .description("PENDIENTE")
                .build();
    }

    public static StatusAvailability buildStatusApproved() {
        return StatusAvailability.builder()
                .id(2L)
                .description("APROBADO")
                .build();
    }

    public static StatusAvailability buildStatusRejected() {
        return StatusAvailability.builder()
                .id(3L)
                .description("RECHAZADO")
                .build();
    }

    // ─── Disponibilidad ──────────────────────────────────────────────────────

    public static Availability buildAvailability(User user, Semester semester, StatusAvailability status) {
        Availability a = new Availability();
        a.setId(10L);
        a.setUser(user);
        a.setSemester(semester);
        a.setDayOfWeek(DayOfWeekEnum.LUNES);
        a.setStartTime(LocalTime.of(8, 0));
        a.setStatusAvailability(status);
        return a;
    }

    // ─── Disponibilidad individual ───────────────────────────────────────────

    public static IndividualAvailability buildIndividualAvailability(User user, Semester semester) {
        return IndividualAvailability.builder()
                .id(1L)
                .user(user)
                .semester(semester)
                .isActive(true)
                .build();
    }

    // ─── Áreas ───────────────────────────────────────────────────────────────

    public static Area buildArea() {
        Area area = new Area();
        area.setId(1L);
        area.setDescription("MATEMATICAS");
        return area;
    }

    // ─── Niveles de asignatura ───────────────────────────────────────────────

    public static LevelSubject buildLevelSubject() {
        LevelSubject level = new LevelSubject();
        level.setId(1L);
        level.setDescription("PREGRADO");
        return level;
    }

    // ─── Asignaturas ─────────────────────────────────────────────────────────

    public static Subject buildSubject(Area area, LevelSubject level) {
        return Subject.builder()
                .id(1L)
                .name("Cálculo I")
                .codigo("MAT001")
                .area(area)
                .levelSubject(level)
                .build();
    }

    // ─── Grupos ──────────────────────────────────────────────────────────────

    public static Group buildGroup(Subject subject, Semester semester, User docente) {
        Group group = new Group();
        group.setId(1L);
        group.setCode("G01");
        group.setSubject(subject);
        group.setSemester(semester);
        group.setDocente(docente);
        group.setMax_students("30");
        group.setEnrolled("20");
        return group;
    }

    // ─── Schedules ───────────────────────────────────────────────────────────

    public static ScheduleDTO buildScheduleDTO() {
        return new ScheduleDTO(null, 8, DayOfWeekEnum.LUNES);
    }

    // ─── Tipo de documento ───────────────────────────────────────────────────

    public static TypeDocument buildTypeDocument() {
        TypeDocument td = new TypeDocument();
        td.setId(1L);
        td.setDescription("CEDULA DE CIUDADANIA");
        return td;
    }

    // ─── Sigla ───────────────────────────────────────────────────────────────

    public static Sigla buildSigla() {
        Sigla s = Sigla.builder()
                .id(1L)
                .sigla("CC")
                .build();
        return s;
    }

    // ─── DTOs de request ─────────────────────────────────────────────────────

    public static LoginRequest buildLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setDocumento("11111111");
        req.setPassword("password123");
        return req;
    }

    public static RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setDocumento("33333333");
        req.setEmail("nuevo@test.com");
        req.setPassword("pass123");
        req.setFirstName("Nuevo");
        req.setLastName("Usuario");
        req.setIdTipoDocumento(1L);
        req.setIdsRoles(List.of(3L));
        return req;
    }

    public static SemesterDTO buildSemesterDTO() {
        SemesterDTO dto = new SemesterDTO();
        dto.setDescription("2025-2");
        dto.setStartDate(LocalDate.of(2025, 7, 15));
        dto.setEndDate(LocalDate.of(2025, 12, 15));
        dto.setAvailability(false);
        return dto;
    }

    public static AvailabilityDTO buildAvailabilityDTO(StatusAvailability status) {
        AvailabilityBlockDTO block = AvailabilityBlockDTO.builder()
                .hour(8)
                .statusId(status.getId())
                .build();
        java.util.Map<DayOfWeekEnum, List<AvailabilityBlockDTO>> map = new java.util.HashMap<>();
        map.put(DayOfWeekEnum.LUNES, List.of(block));
        return AvailabilityDTO.builder().disponibilidad(map).build();
    }
}
