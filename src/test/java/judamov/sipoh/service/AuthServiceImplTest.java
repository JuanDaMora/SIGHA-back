package judamov.sipoh.service;

import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.impl.AuthServiceImpl;
import judamov.sipoh.service.impl.EmailServiceImpl;
import judamov.sipoh.service.impl.JwtServiceImpl;
import judamov.sipoh.service.impl.UserRolServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Tests unitarios")
class AuthServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private ITypeDocumentRepository typeDocumentRepository;
    @Mock private IRoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IAccessControlRepository accessControlRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtServiceImpl jwtServiceImpl;
    @Mock private IUserAreaRepository userAreaRepository;
    @Mock private UserRolServiceImpl userRolService;
    @Mock private IAreaRepository areaRepository;
    @Mock private EmailServiceImpl emailService;
    @Mock private IProgramRepository programRepository;
    @Mock private IUserRoleRepository userRoleRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User adminUser;
    private User teacherUser;
    private Program program;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "currentSchema", "ing_sistemas");
        adminUser = TestDataFactory.buildAdminUser();
        teacherUser = TestDataFactory.buildTeacherUser();
        program = TestDataFactory.buildProgram();
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login - retorna token cuando las credenciales son correctas")
    void shouldReturnTokenOnSuccessfulLogin() {
        LoginRequest request = TestDataFactory.buildLoginRequest();
        adminUser.setTokenHash("existingHash");

        AccessControl accessControl = AccessControl.builder()
                .user(adminUser)
                .lastLogin(new Date())
                .build();

        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.of(adminUser));
        when(jwtServiceImpl.getToken(adminUser)).thenReturn("generated.jwt.token");
        when(jwtServiceImpl.hashToken("generated.jwt.token")).thenReturn("newHash");
        when(accessControlRepository.findOneByUser(adminUser)).thenReturn(Optional.of(accessControl));

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("generated.jwt.token");
        assertThat(response.getForcePasswordReset()).isFalse();
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("login - forcePasswordReset=true cuando tokenHash es null")
    void shouldForcePasswordResetWhenTokenHashIsNull() {
        LoginRequest request = TestDataFactory.buildLoginRequest();
        adminUser.setTokenHash(null);

        AccessControl accessControl = AccessControl.builder()
                .user(adminUser)
                .lastLogin(new Date())
                .build();

        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.of(adminUser));
        when(jwtServiceImpl.getToken(adminUser)).thenReturn("jwt.token");
        when(accessControlRepository.findOneByUser(adminUser)).thenReturn(Optional.of(accessControl));

        AuthResponse response = authService.login(request);

        assertThat(response.getForcePasswordReset()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - lanza excepción NOT_FOUND cuando el usuario no existe")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        LoginRequest request = TestDataFactory.buildLoginRequest();
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("login - lanza UNAUTHORIZED cuando la contraseña es incorrecta")
    void shouldThrowUnauthorizedWhenWrongPassword() {
        LoginRequest request = TestDataFactory.buildLoginRequest();
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.of(adminUser));
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("contraseña ingresada es incorrecta");
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register - registra usuario exitosamente cuando los datos son válidos")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        TypeDocument typeDocument = TestDataFactory.buildTypeDocument();
        Role role = TestDataFactory.buildRoleProfesor();

        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.of(typeDocument));
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findOneById(3L)).thenReturn(Optional.of(role));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.sendEmail(any(EmailRequestDTO.class))).thenReturn(true);

        RegisterResponse response = authService.register(request, adminUser.getId());

        assertThat(response.getPassword()).isEqualTo(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(accessControlRepository).save(any(AccessControl.class));
    }

    @Test
    @DisplayName("register - lanza FORBIDDEN cuando el usuario no es administrador")
    void shouldThrowForbiddenWhenNotAdmin() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request, teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    @DisplayName("register - lanza BAD_REQUEST cuando el documento ya existe")
    void shouldThrowBadRequestWhenDocumentAlreadyExists() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        TypeDocument typeDocument = TestDataFactory.buildTypeDocument();

        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.of(typeDocument));
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> authService.register(request, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Ya existe un usuario con el documento");
    }

    @Test
    @DisplayName("register - lanza BAD_REQUEST cuando el email ya existe")
    void shouldThrowBadRequestWhenEmailAlreadyExists() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        TypeDocument typeDocument = TestDataFactory.buildTypeDocument();

        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.of(typeDocument));
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmail(request.getEmail())).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> authService.register(request, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Ya existe un usuario con el correo");
    }

    // ─── changePassword ───────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword - actualiza la contraseña y retorna nuevo token")
    void shouldChangePasswordAndReturnNewToken() {
        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setDocumento(adminUser.getDocumento());
        request.setLastPassword("oldPass");
        request.setPassword("newPass");

        when(userRepository.findOneByDocumento(adminUser.getDocumento())).thenReturn(Optional.of(adminUser));
        when(jwtServiceImpl.getToken(adminUser)).thenReturn("new.token");
        when(jwtServiceImpl.hashToken("new.token")).thenReturn("newHash");
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        ChangePasswordResponse response = authService.changePassword(request);

        assertThat(response.getToken()).isEqualTo("new.token");
        verify(userRepository).save(adminUser);
        assertThat(adminUser.getPassword()).isEqualTo("encodedNewPass");
    }

    @Test
    @DisplayName("changePassword - lanza NOT_FOUND cuando el usuario no existe")
    void shouldThrowNotFoundOnChangePasswordWhenUserNotFound() {
        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setDocumento("99999999");
        when(userRepository.findOneByDocumento("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ─── getAllUsers ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers - retorna lista de usuarios con datos enriquecidos")
    void shouldReturnAllUsersWithEnrichedData() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program)).thenReturn(Optional.of(List.of()));
        when(accessControlRepository.findByUserId(adminUser.getId())).thenReturn(Optional.empty());
        when(userAreaRepository.findByUserId(adminUser.getId())).thenReturn(List.of());

        List<UserDTO> result = authService.getAllUsers();

        assertThat(result).hasSize(1);
    }

    // ─── getUserById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById - retorna UserDTO cuando el solicitante es administrador")
    void shouldReturnUserDtoWhenAdminRequestsOtherUser() {
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(teacherUser, program)).thenReturn(Optional.of(List.of()));
        when(accessControlRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.empty());
        when(userAreaRepository.findByUserId(teacherUser.getId())).thenReturn(List.of());

        UserDTO result = authService.getUserById(teacherUser.getId(), adminUser.getId());

        assertThat(result.getId()).isEqualTo(teacherUser.getId());
        assertThat(result.getDocumento()).isEqualTo(teacherUser.getDocumento());
    }

    @Test
    @DisplayName("getUserById - lanza FORBIDDEN cuando el solicitante no es administrador")
    void shouldThrowForbiddenWhenNonAdminGetsUserById() {
        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(userRolService.hasAdminPrivileges(teacherUser)).thenReturn(false);

        assertThatThrownBy(() -> authService.getUserById(adminUser.getId(), teacherUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("permisos");
    }

    // ─── getOwnUserData ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getOwnUserData - retorna datos del usuario autenticado con roles del programa actual")
    void shouldReturnOwnUserData() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(teacherUser.getId(), program.getId()))
                .thenReturn(Optional.of(List.of()));
        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(accessControlRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.empty());
        when(userAreaRepository.findByUserId(teacherUser.getId())).thenReturn(List.of());

        UserDTO result = authService.getOwnUserData(teacherUser.getId());

        assertThat(result.getId()).isEqualTo(teacherUser.getId());
    }

    @Test
    @DisplayName("getOwnUserData - lanza NOT_FOUND cuando el usuario no existe")
    void shouldThrowNotFoundOnGetOwnUserDataWhenUserMissing() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRoleRepository.findAllByUserAndProgram(99L, program.getId())).thenReturn(Optional.of(List.of()));
        when(userRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getOwnUserData(99L))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ─── updateUserMe ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUserMe - actualiza campos básicos del usuario autenticado")
    void shouldUpdateOwnUserBasicFields() {
        TypeDocument typeDoc = TestDataFactory.buildTypeDocument();
        UserBasicUpdateDTO dto = UserBasicUpdateDTO.builder()
                .id(teacherUser.getId())
                .email("nuevo@mail.com")
                .idTipoDocumento(typeDoc.getId())
                .documento(teacherUser.getDocumento())
                .firstName("Nuevo")
                .lastName("Nombre")
                .build();

        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(typeDocumentRepository.findById(typeDoc.getId())).thenReturn(Optional.of(typeDoc));

        Boolean result = authService.updateUserMe(teacherUser.getId(), dto);

        assertThat(result).isTrue();
        verify(userRepository).save(teacherUser);
        assertThat(teacherUser.getEmail()).isEqualTo("nuevo@mail.com");
    }

    @Test
    @DisplayName("registerBulkUsers - retorna true cuando la lista está vacía")
    void shouldReturnTrueWhenBulkRegisterListIsEmpty() {
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);

        Boolean result = authService.registerBulkUsers(adminUser.getId(), List.of(), false);

        assertThat(result).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - lanza NOT_FOUND cuando no existe registro de AccessControl")
    void shouldThrowNotFoundWhenAccessControlMissingOnLogin() {
        LoginRequest request = TestDataFactory.buildLoginRequest();
        adminUser.setTokenHash("h");
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.of(adminUser));
        when(jwtServiceImpl.getToken(adminUser)).thenReturn("t");
        when(jwtServiceImpl.hashToken("t")).thenReturn("hash");
        when(accessControlRepository.findOneByUser(adminUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("registro de acceso");
    }

    @Test
    @DisplayName("register - lanza BAD_REQUEST cuando el tipo de documento no existe")
    void shouldThrowBadRequestWhenTypeDocumentNotFoundOnRegister() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Tipo de documento no encontrado");
    }

    @Test
    @DisplayName("register - lanza BAD_REQUEST cuando un rol no existe")
    void shouldThrowBadRequestWhenRoleNotFoundOnRegister() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        TypeDocument td = TestDataFactory.buildTypeDocument();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.of(td));
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(roleRepository.findOneById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Rol no encontrado");
    }

    @Test
    @DisplayName("register - persiste áreas cuando idsAreas viene informado")
    void shouldSaveUserAreasWhenRegisterWithAreaIds() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        request.setIdsAreas(List.of(1L, 2L));
        TypeDocument td = TestDataFactory.buildTypeDocument();
        Role role = TestDataFactory.buildRoleProfesor();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.of(td));
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findOneById(3L)).thenReturn(Optional.of(role));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.sendEmail(any(EmailRequestDTO.class))).thenReturn(true);
        when(accessControlRepository.save(any(AccessControl.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request, adminUser.getId());

        verify(userAreaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("register - lanza INTERNAL_SERVER_ERROR cuando falla el guardado de AccessControl")
    void shouldThrowInternalErrorWhenAccessControlSaveFails() {
        RegisterRequest request = TestDataFactory.buildRegisterRequest();
        TypeDocument td = TestDataFactory.buildTypeDocument();
        Role role = TestDataFactory.buildRoleProfesor();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(typeDocumentRepository.findOneById(request.getIdTipoDocumento())).thenReturn(Optional.of(td));
        when(userRepository.findOneByDocumento(request.getDocumento())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findOneById(3L)).thenReturn(Optional.of(role));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.sendEmail(any(EmailRequestDTO.class))).thenReturn(true);
        doThrow(new RuntimeException("db")).when(accessControlRepository).save(any(AccessControl.class));

        assertThatThrownBy(() -> authService.register(request, adminUser.getId()))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("control de acceso");
    }

    @Test
    @DisplayName("getAllUsers - lanza error cuando el programa actual no está configurado")
    void shouldThrowWhenProgramMissingOnGetAllUsers() {
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getAllUsers())
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("configuración");
    }

    @Test
    @DisplayName("getAllUsers - enriquece lastLogin e idAreas cuando existen datos")
    void shouldEnrichLastLoginAndAreasOnGetAllUsers() {
        AccessControl ac = AccessControl.builder().user(adminUser).lastLogin(new Date()).build();
        Area area = TestDataFactory.buildArea();
        UserArea ua = new UserArea(null, adminUser, area, null, null);
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(userRoleRepository.findAllByUserAndProgram(adminUser, program)).thenReturn(Optional.of(List.of()));
        when(accessControlRepository.findByUserId(adminUser.getId())).thenReturn(Optional.of(ac));
        when(userAreaRepository.findByUserId(adminUser.getId())).thenReturn(List.of(ua));

        List<UserDTO> result = authService.getAllUsers();

        assertThat(result.get(0).getLastLogin()).isNotNull();
        assertThat(result.get(0).getIdAreas()).containsExactly(area.getId());
    }

    @Test
    @DisplayName("updateUser - actualiza roles y áreas cuando el admin envía listas válidas")
    void shouldUpdateUserWithRolesAndAreas() {
        User target = TestDataFactory.buildTeacherUser();
        target.setUserRoles(new ArrayList<>());
        target.setUserAreas(new ArrayList<>());
        TypeDocument td = TestDataFactory.buildTypeDocument();
        Role role = TestDataFactory.buildRoleProfesor();
        Area area = TestDataFactory.buildArea();
        UserDTO dto = UserDTO.builder()
                .email("t@t.com")
                .idTipoDocumento(td.getId())
                .documento(target.getDocumento())
                .firstName("A")
                .lastName("B")
                .isActive(true)
                .idsRoles(List.of(role.getId()))
                .idAreas(List.of(area.getId()))
                .build();

        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneById(target.getId())).thenReturn(Optional.of(target));
        when(typeDocumentRepository.findById(td.getId())).thenReturn(Optional.of(td));
        when(roleRepository.findAllById(dto.getIdsRoles())).thenReturn(List.of(role));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(areaRepository.findAllById(dto.getIdAreas())).thenReturn(List.of(area));

        assertThat(authService.updateUser(adminUser.getId(), target.getId(), dto)).isTrue();
        verify(userRepository).save(target);
        assertThat(target.getUserRoles()).hasSize(1);
        assertThat(target.getUserAreas()).hasSize(1);
    }

    @Test
    @DisplayName("updateUser - lanza BAD_REQUEST cuando falta algún rol")
    void shouldThrowBadRequestWhenSomeRolesMissingOnUpdateUser() {
        User target = TestDataFactory.buildTeacherUser();
        target.setUserRoles(new ArrayList<>());
        TypeDocument td = TestDataFactory.buildTypeDocument();
        UserDTO dto = UserDTO.builder()
                .email("t@t.com")
                .idTipoDocumento(td.getId())
                .documento("1")
                .firstName("A")
                .lastName("B")
                .isActive(true)
                .idsRoles(List.of(3L, 99L))
                .build();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneById(target.getId())).thenReturn(Optional.of(target));
        when(typeDocumentRepository.findById(td.getId())).thenReturn(Optional.of(td));
        when(roleRepository.findAllById(dto.getIdsRoles())).thenReturn(List.of(TestDataFactory.buildRoleProfesor()));

        assertThatThrownBy(() -> authService.updateUser(adminUser.getId(), target.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("roles no existen");
    }

    @Test
    @DisplayName("updateUser - lanza BAD_REQUEST cuando falta algún área")
    void shouldThrowBadRequestWhenSomeAreasMissingOnUpdateUser() {
        User target = TestDataFactory.buildTeacherUser();
        target.setUserAreas(new ArrayList<>());
        TypeDocument td = TestDataFactory.buildTypeDocument();
        UserDTO dto = UserDTO.builder()
                .email("t@t.com")
                .idTipoDocumento(td.getId())
                .documento("1")
                .firstName("A")
                .lastName("B")
                .isActive(true)
                .idAreas(List.of(1L, 99L))
                .build();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneById(target.getId())).thenReturn(Optional.of(target));
        when(typeDocumentRepository.findById(td.getId())).thenReturn(Optional.of(td));
        when(areaRepository.findAllById(dto.getIdAreas())).thenReturn(List.of(TestDataFactory.buildArea()));

        assertThatThrownBy(() -> authService.updateUser(adminUser.getId(), target.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("áreas no existen");
    }

    @Test
    @DisplayName("updateUser - lanza NOT_FOUND cuando el usuario objetivo no existe")
    void shouldThrowNotFoundWhenTargetUserMissingOnUpdateUser() {
        UserDTO dto = UserDTO.builder()
                .idTipoDocumento(1L)
                .email("x")
                .documento("1")
                .firstName("A")
                .lastName("B")
                .isActive(true)
                .build();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUser(adminUser.getId(), 99L, dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("updateUser - lanza NOT_FOUND cuando el tipo de documento no existe")
    void shouldThrowNotFoundWhenTypeDocumentMissingOnUpdateUser() {
        User target = TestDataFactory.buildTeacherUser();
        UserDTO dto = UserDTO.builder()
                .idTipoDocumento(99L)
                .email("x")
                .documento("1")
                .firstName("A")
                .lastName("B")
                .isActive(true)
                .build();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneById(target.getId())).thenReturn(Optional.of(target));
        when(typeDocumentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUser(adminUser.getId(), target.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Tipo de documento no encontrado");
    }

    @Test
    @DisplayName("changePassword - lanza UNAUTHORIZED cuando la contraseña actual es incorrecta")
    void shouldThrowUnauthorizedWhenLastPasswordWrong() {
        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setDocumento(adminUser.getDocumento());
        request.setLastPassword("wrong");
        request.setPassword("new");
        when(userRepository.findOneByDocumento(adminUser.getDocumento())).thenReturn(Optional.of(adminUser));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("contraseña ingresada es incorrecta");
    }

    @Test
    @DisplayName("registerBulkUsers - crea usuario, control de acceso y envía correo para un documento nuevo")
    void shouldRegisterOneUserInBulk() {
        BulkUserDTO bulk = BulkUserDTO.builder()
                .documento("123-456-7890")
                .firstName("N")
                .lastName("L")
                .correoPersonal("nuevo-bulk@test.com")
                .idTipoDocumento(1L)
                .idsRoles(List.of(3L))
                .build();
        TypeDocument td = TestDataFactory.buildTypeDocument();
        Role role = TestDataFactory.buildRoleProfesor();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneByDocumento("1234567890")).thenReturn(Optional.empty());
        when(typeDocumentRepository.findById(1L)).thenReturn(Optional.of(td));
        when(roleRepository.findAllById(List.of(3L))).thenReturn(List.of(role));
        when(programRepository.findByCode("ing_sistemas")).thenReturn(Optional.of(program));
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accessControlRepository.save(any(AccessControl.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.sendEmail(any(EmailRequestDTO.class))).thenReturn(true);

        assertThat(authService.registerBulkUsers(adminUser.getId(), List.of(bulk), true)).isTrue();
        verify(userRepository).save(any(User.class));
        verify(emailService).sendEmail(any(EmailRequestDTO.class));
    }

    @Test
    @DisplayName("registerBulkUsers - omite filas con documento en blanco")
    void shouldSkipBlankDocumentInBulk() {
        BulkUserDTO bulk = BulkUserDTO.builder().documento("   ").firstName("X").build();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);

        assertThat(authService.registerBulkUsers(adminUser.getId(), List.of(bulk), false)).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerBulkUsers - lanza BAD_REQUEST cuando faltan roles en carga masiva")
    void shouldThrowBadRequestWhenBulkRolesIncomplete() {
        BulkUserDTO bulk = BulkUserDTO.builder()
                .documento("88888888")
                .firstName("A")
                .lastName("B")
                .correoPersonal("e@test.com")
                .idsRoles(List.of(3L, 99L))
                .build();
        TypeDocument td = TestDataFactory.buildTypeDocument();
        when(userRepository.findOneById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRolService.hasAdminPrivileges(adminUser)).thenReturn(true);
        when(userRepository.findOneByDocumento("88888888")).thenReturn(Optional.empty());
        when(typeDocumentRepository.findById(1L)).thenReturn(Optional.of(td));
        when(roleRepository.findAllById(List.of(3L, 99L))).thenReturn(List.of(TestDataFactory.buildRoleProfesor()));

        assertThatThrownBy(() -> authService.registerBulkUsers(adminUser.getId(), List.of(bulk), false))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("roles no existen");
    }

    @Test
    @DisplayName("updateUserMe - lanza NOT_FOUND cuando el usuario no existe")
    void shouldThrowNotFoundOnUpdateUserMeWhenUserMissing() {
        UserBasicUpdateDTO dto = UserBasicUpdateDTO.builder().idTipoDocumento(1L).build();
        when(userRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUserMe(99L, dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("updateUserMe - lanza NOT_FOUND cuando el tipo de documento no existe")
    void shouldThrowNotFoundOnUpdateUserMeWhenTypeDocMissing() {
        UserBasicUpdateDTO dto = UserBasicUpdateDTO.builder().idTipoDocumento(99L).build();
        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(typeDocumentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUserMe(teacherUser.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Tipo de documento no encontrado");
    }

    @Test
    @DisplayName("updateUserMe - lanza INTERNAL_SERVER_ERROR cuando save falla")
    void shouldThrowInternalErrorWhenUpdateUserMeSaveFails() {
        TypeDocument td = TestDataFactory.buildTypeDocument();
        UserBasicUpdateDTO dto = UserBasicUpdateDTO.builder()
                .idTipoDocumento(td.getId())
                .email("x@x.com")
                .documento(teacherUser.getDocumento())
                .firstName("A")
                .lastName("B")
                .build();
        when(userRepository.findOneById(teacherUser.getId())).thenReturn(Optional.of(teacherUser));
        when(typeDocumentRepository.findById(td.getId())).thenReturn(Optional.of(td));
        doThrow(new RuntimeException("db")).when(userRepository).save(teacherUser);

        assertThatThrownBy(() -> authService.updateUserMe(teacherUser.getId(), dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Error al actualizar el usuario");
    }
}
