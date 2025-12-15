package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.mappers.UserMapper;
import judamov.sipoh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl {
    private final IUserRepository userRepository;
    private final ITypeDocumentRepository typeDocumentRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAccessControlRepository accessControlRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtServiceImpl jwtServiceImpl;
    private final IUserAreaRepository userAreaRepository;
    private final UserRolServiceImpl userRolService;
    private final IAreaRepository areaRepository;
    private final EmailServiceImpl emailService;
    private final IProgramRepository programRepository;
    private final IUserRoleRepository userRoleRepository;
    
    @Value("${spring.jpa.properties.hibernate.default_schema:ing_sistemas}")
    private String currentSchema;


    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();


    /**
     * Obtiene todos los usuarios del sistema con sus últimos accesos y áreas asociadas.
     *
     * @return lista de objetos {@link UserDTO}
     */
    public List<UserDTO> getAllUsers() {
        Program currentProgram = getCurrentProgram();
        
        List<UserDTO> userDTOList = userRepository.findAll()
                .stream()
                .map(user -> {
                    // Filtrar roles por programa actual
                    List<UserRol> userRolesForProgram = userRoleRepository.findAllByUserAndProgram(user, currentProgram)
                            .orElse(new ArrayList<>());
                    return UserMapper.userToUserDTO(user, userRolesForProgram);
                })
                .collect(Collectors.toList());
        
        for (UserDTO userDTO : userDTOList) {
            // lastLogin
            accessControlRepository.findByUserId(userDTO.getId())
                    .ifPresent(accessControl -> userDTO.setLastLogin(accessControl.getLastLogin()));
            // idsAreas
            List<Long> areaIds = userAreaRepository.findByUserId(userDTO.getId()).stream()
                    .map(userArea -> userArea.getArea().getId())
                    .toList();
            userDTO.setIdAreas(areaIds);
        }
        return userDTOList;
    }

    /**
     * Autentica al usuario y genera un token JWT. También actualiza el último acceso y el token hash.
     *
     * @param request contiene documento y contraseña
     * @return respuesta con token y bandera de forzar cambio de contraseña
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findOneByDocumento(request.getDocumento())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con documento: " + request.getDocumento()));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getDocumento(), request.getPassword()));
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "La contraseña ingresada es incorrecta");
        }
        Boolean forcePasswordReset;
        String token = jwtServiceImpl.getToken(user);
        if(user.getTokenHash()==null){
            forcePasswordReset= true;
        }else{
            forcePasswordReset = false;
            String tokenHash = jwtServiceImpl.hashToken(token);

            user.setTokenHash(tokenHash);
            userRepository.save(user);
        }

        AccessControl accessControl= accessControlRepository.findOneByUser(user)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "Access control no econtrado"));
        accessControl.setLastLogin(new Date());
        accessControlRepository.save(accessControl);

        return AuthResponse.builder()
                .token(token)
                .forcePasswordReset(forcePasswordReset)
                .build();
    }
    /**
     * Registra un nuevo usuario con sus roles, áreas y control de acceso inicial.
     *
     * @param request datos del usuario a registrar
     * @return respuesta con la contraseña (sin encriptar)
     */
    public RegisterResponse register(RegisterRequest request, Long userId) {
        User admin = userRepository.findOneById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.BAD_REQUEST, "Bad Request"));

        if (!userRolService.hasAdminPrivileges(admin)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "Usuario no autorizado");
        }

        TypeDocument typeDocument = typeDocumentRepository.findOneById(request.getIdTipoDocumento())
                .orElseThrow(() -> new GenericAppException(HttpStatus.BAD_REQUEST,
                        "Tipo de documento no encontrado con id: " + request.getIdTipoDocumento()));

        userRepository.findOneByDocumento(request.getDocumento()).ifPresent(u -> {
            throw new GenericAppException(HttpStatus.BAD_REQUEST,
                    "Ya existe un usuario con el documento: " + request.getDocumento());
        });

        userRepository.findOneByEmail(request.getEmail()).ifPresent(u -> {
            throw new GenericAppException(HttpStatus.BAD_REQUEST,
                    "Ya existe un usuario con el correo: " + request.getEmail());
        });

        User user = User.builder()
                .documento(request.getDocumento())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .typeDocument(typeDocument)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .active(true)
                .build();

        Program program = getCurrentProgram();
        List<UserRol> userRoles = request.getIdsRoles().stream().map(roleId -> {
            Role role = roleRepository.findOneById(roleId)
                    .orElseThrow(() -> new GenericAppException(HttpStatus.BAD_REQUEST,
                            "Rol no encontrado con id: " + roleId));
            return new UserRol(null, user, role, program, null, null);
        }).collect(Collectors.toList());

        user.setUserRoles(userRoles);

        try {
            User savedUser = userRepository.save(user); // Ya creado previamente

            String fullName = savedUser.getFirstName() + " " + savedUser.getLastName();

            EmailRequestDTO emailDTO = EmailRequestDTO.builder()
                    .nombre(fullName)
                    .documento(savedUser.getDocumento())
                    .password(request.getPassword()) // asegúrate de conservar la password generada
                    .email(request.getEmail()) // o correoInstitucional, según tu lógica
                    .fake(false) // o true si quieres simular
                    .build();

            emailService.sendEmail(emailDTO);

        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado al guardar el usuario");
        }

        // Asociar áreas si vienen en la solicitud
        if (request.getIdsAreas() != null && !request.getIdsAreas().isEmpty()) {
            List<UserArea> userAreas = request.getIdsAreas().stream().map(areaId -> {
                Area area = new Area();
                area.setId(areaId);
                return new UserArea(null, user, area, null, null);
            }).collect(Collectors.toList());

            userAreaRepository.saveAll(userAreas);
        }

        // Registrar AccessControl
        AccessControl newAccessControl = AccessControl.builder()
                .user(user)
                .lastLogin(new Date())
                .build();
        try {
            accessControlRepository.save(newAccessControl);

        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado al guardar el access control");
        }


        return RegisterResponse.builder()
                .password(request.getPassword())
                .build();
    }
    /**
     * Cambia la contraseña de un usuario autenticado.
     *
     * @param request contiene documento, contraseña actual y nueva contraseña
     * @return respuesta con el nuevo token
     */
    public ChangePasswordResponse changePassword(ChangePasswordDTO request) {
        User user = userRepository.findOneByDocumento(request.getDocumento())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con documento: " + request.getDocumento()));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getDocumento(), request.getLastPassword()));
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "La contraseña ingresada es incorrecta");
        }

        String token = jwtServiceImpl.getToken(user);
        String tokenHash = jwtServiceImpl.hashToken(token);

        user.setTokenHash(tokenHash);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return ChangePasswordResponse.builder()
                .token(token)
                .build();
    }
    /**
     * Retorna los datos de un usuario por ID solo si tiene un rol permitido.
     *
     * @param id ID del usuario a consultar
     * @return objeto {@link UserDTO} con datos, áreas y login
     */
    public UserDTO getUserById(Long id, Long userId) {
        User requester = userRepository.findOneById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        if (!userRolService.hasAdminPrivileges(requester)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para consultar esta información");
        }

        User user = userRepository.findOneById(id)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Filtrar roles por programa actual
        Program currentProgram = getCurrentProgram();
        List<UserRol> userRolesForProgram = userRoleRepository.findAllByUserAndProgram(user, currentProgram)
                .orElse(new ArrayList<>());

        UserDTO userDTO = UserMapper.userToUserDTO(user, userRolesForProgram);

        accessControlRepository.findByUserId(id)
                .ifPresent(ac -> userDTO.setLastLogin(ac.getLastLogin()));

        List<Long> areaIds = userAreaRepository.findByUserId(id).stream()
                .map(userArea -> userArea.getArea().getId())
                .toList();
        userDTO.setIdAreas(areaIds);

        return userDTO;
    }

    /**
     * Retorna los datos del usuario autenticado sin validaciones de rol.
     * Filtra roles por el programa actual del backend.
     *
     * @param userId ID del usuario autenticado
     * @return objeto {@link UserDTO} con datos personales, login y áreas
     */
    public UserDTO getOwnUserData(Long userId) {
        // Filtrar roles por programa actual
        Program currentProgram = getCurrentProgram();
        List<UserRol> userRolesForProgram = userRoleRepository.findAllByUserAndProgram(userId, currentProgram.getId())
                .orElse(new ArrayList<>());

        User user = userRepository.findOneById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        UserDTO userDTO = UserMapper.userToUserDTO(user, userRolesForProgram);

        accessControlRepository.findByUserId(userId)
                .ifPresent(ac -> userDTO.setLastLogin(ac.getLastLogin()));

        List<Long> areaIds = userAreaRepository.findByUserId(userId).stream()
                .map(userArea -> userArea.getArea().getId())
                .toList();
        userDTO.setIdAreas(areaIds);

        return userDTO;
    }



    /**
     * Actualiza un usuario si tiene uno de los roles permitidos.
     *
     * @param id del usuario a actualizar
     * @param userDTO nuevos datos
     * @return true si fue actualizado correctamente
     */
    @Transactional
    public Boolean updateUser(Long adminId, Long id, UserDTO userDTO) {
        User admin = userRepository.findOneById(adminId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        User user = userRepository.findOneById(id)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!userRolService.hasAdminPrivileges(admin)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para actualizar este usuario");
        }

        // Actualizar campos básicos
        TypeDocument typeDocument = typeDocumentRepository.findById(userDTO.getIdTipoDocumento())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Tipo de documento no encontrado"));

        user.setEmail(userDTO.getEmail());
        user.setDocumento(userDTO.getDocumento());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setTypeDocument(typeDocument);
        user.setActive(userDTO.getIsActive());

        // Actualizar roles si vienen en el DTO
        if (userDTO.getIdsRoles() != null) {
            List<Role> roles = roleRepository.findAllById(userDTO.getIdsRoles());
            if (roles.size() != userDTO.getIdsRoles().size()) {
                throw new GenericAppException(HttpStatus.BAD_REQUEST, "Uno o más roles no existen");
            }

            // Limpiar roles antiguos
            user.getUserRoles().clear();

            // Asignar nuevos
            Program program = getCurrentProgram();
            List<UserRol> nuevosRoles = roles.stream()
                    .map(role -> new UserRol(null, user, role, program, null, null))
                    .toList();

            user.getUserRoles().addAll(nuevosRoles);
        }

        // Actualizar áreas si vienen en el DTO
        if (userDTO.getIdAreas() != null) {
            List<Area> areas = areaRepository.findAllById(userDTO.getIdAreas());
            if (areas.size() != userDTO.getIdAreas().size()) {
                throw new GenericAppException(HttpStatus.BAD_REQUEST, "Una o más áreas no existen");
            }

            // Limpiar áreas anteriores
            user.getUserAreas().clear();

            // Asignar nuevas
            List<UserArea> nuevasAreas = areas.stream()
                    .map(area -> new UserArea(null, user, area, null, null))
                    .toList();

            user.getUserAreas().addAll(nuevasAreas);
        }

        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error actualizando el usuario");
        }

        return true;
    }
@Transactional
    public Boolean updateUserMe (Long userId,UserBasicUpdateDTO userDTO){
        User user = userRepository.findOneById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        TypeDocument typeDoc = typeDocumentRepository.findById(userDTO.getIdTipoDocumento())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Tipo de documento no encontrado"));
        UserMapper.updateUserBasicFields(user, userDTO, typeDoc);
        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error actualizando el usuario");
        }
        return true;
    }
    @Transactional
    public Boolean registerBulkUsers(Long userId, List<BulkUserDTO> usuariosDTO, Boolean isFakeEmail) {
        User admin = userRepository.findOneById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        if (!userRolService.hasAdminPrivileges(admin)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para actualizar este usuario");
        }

        for (BulkUserDTO dto : usuariosDTO) {
            if (dto.getDocumento() == null || dto.getDocumento().isBlank()) continue;

            String documentoLimpio = dto.getDocumento().replaceAll("[^\\d]", "");

            if (userRepository.findOneByDocumento(documentoLimpio).isPresent()) continue;

            Long tipoDocId = dto.getIdTipoDocumento() != null ? dto.getIdTipoDocumento() : 1L;
            List<Long> idsRoles = (dto.getIdsRoles() != null && !dto.getIdsRoles().isEmpty())
                    ? dto.getIdsRoles()
                    : List.of(3L); // por defecto, rol docente

            TypeDocument tipoDocumento = typeDocumentRepository.findById(tipoDocId)
                    .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Tipo de documento no encontrado con id: " + tipoDocId));

            List<Role> roles = roleRepository.findAllById(idsRoles);
            if (roles.size() != idsRoles.size()) {
                throw new GenericAppException(HttpStatus.BAD_REQUEST, "Uno o más roles no existen");
            }

            String plainPassword = generateRandomPassword();

            User user = User.builder()
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    .email(dto.getCorreoInstitucional() != null && !dto.getCorreoInstitucional().isBlank()
                            ? dto.getCorreoInstitucional()
                            : dto.getCorreoPersonal())
                    .documento(documentoLimpio)
                    .password(passwordEncoder.encode(plainPassword))
                    .typeDocument(tipoDocumento)
                    .active(true)
                    .build();

            Program program = getCurrentProgram();
            List<UserRol> userRoles = roles.stream()
                    .map(role -> new UserRol(null, user, role, program, null, null))
                    .toList();

            user.setUserRoles(userRoles);

            // Áreas (si vienen)
            List<UserArea> userAreas = new ArrayList<>();
            if (dto.getIdAreas() != null && !dto.getIdAreas().isEmpty()) {
                List<Area> areas = areaRepository.findAllById(dto.getIdAreas());
                if (areas.size() != dto.getIdAreas().size()) {
                    throw new GenericAppException(HttpStatus.BAD_REQUEST, "Una o más áreas no existen");
                }
                userAreas = areas.stream()
                        .map(area -> new UserArea(null, user, area, null, null))
                        .toList();
            }
            user.setUserAreas(userAreas);

            userRepository.save(user);

            accessControlRepository.save(AccessControl.builder()
                    .user(user)
                    .lastLogin(new Date())
                    .build());

                EmailRequestDTO emailRequest = EmailRequestDTO.builder()
                        .nombre(dto.getFirstName() + " " + dto.getLastName())
                        .documento(documentoLimpio)
                        .email(user.getEmail())
                        .password(plainPassword)
                        .fake(isFakeEmail)
                        .build();

                emailService.sendEmail(emailRequest);
        }
        return true;
    }

    private String generateRandomPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_POOL.length());
            password.append(CHAR_POOL.charAt(index));
        }
        return password.toString();
    }
    
    /**
     * Obtiene el Program correspondiente al schema actual configurado
     */
    private Program getCurrentProgram() {
        return programRepository.findByCode(currentSchema)
                .orElseThrow(() -> new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Programa no encontrado para el schema: " + currentSchema));
    }
}
