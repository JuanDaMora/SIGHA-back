package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.AvailabilityBlockDTO;
import judamov.sipoh.dto.AvailabilityDTO;
import judamov.sipoh.dto.GlobalAvabilityDTO;
import judamov.sipoh.entity.*;
import judamov.sipoh.enums.DayOfWeekEnum;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.interfaces.IAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * Servicio que gestiona la disponibilidad horaria de los docentes.
 * Incluye lógica de creación, eliminación, y consulta de disponibilidad por semestre.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements IAvailabilityService {

    private final IAvailabilityRepository availabilityRepository;
    private final IUserRoleRepository userRoleRepository;
    private final IUserRepository userRepository;
    private final ISemesterRepository semesterRepository;
    private final IStatusAvailabilityRepository statusAvailabilityRepository;
    private final UserRolServiceImpl userRolService;
    private final IUserAreaRepository userAreaRepository;
    private final ISubjectRepository subjectRepository;
    private final IProgramRepository programRepository;
    
    @Value("${spring.jpa.properties.hibernate.default_schema:ing_sistemas}")
    private String currentSchema;


    @Override
    public List<GlobalAvabilityDTO> getGlobalAvailabilityBySubjects(Long semesterId,
                                                                    Long userId,
                                                                    List<Long> subjectIds) {

        validateAdminAccess(userId);
        Semester semester = getSemesterById(semesterId);

        // Si no manda materias -> devolver todos los docentes con availability
        if (subjectIds == null || subjectIds.isEmpty()) {
            return getListGlobalAvailability(semesterId, userId);
        }

        // 1. Obtener las asignaturas seleccionadas
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);

        if (subjects.isEmpty()) {
            log.warn("No se encontraron asignaturas para los ids={}", subjectIds);
            throw new GenericAppException(HttpStatus.NOT_FOUND,
                    "No se encontraron asignaturas para los IDs enviados");
        }

        // 2. Extraer las áreas asociadas a esas asignaturas
        List<Long> areaIds = subjects.stream()
                .map(s -> s.getArea().getId())
                .distinct()
                .toList();

        // 3. Traer todos los docentes que pertenezcan a esas áreas
        List<Long> docentesIds = userAreaRepository.findDocentsByAreaIds(areaIds);

        if (docentesIds.isEmpty()) {
            return List.of(); // sin docentes → lista vacía
        }

        // 4. Para cada docente traer su availability
        List<GlobalAvabilityDTO> result = new ArrayList<>();

        for (Long docenteId : docentesIds) {

            User docente = getUserById(docenteId);

            List<Availability> availabilityList =
                    availabilityRepository.findByUserAndSemester(docente, semester)
                            .orElse(Collections.emptyList());

            GlobalAvabilityDTO dto = buildGlobalAvailabilityDTO(docente.getId(), availabilityList);
            if (availabilityList.isEmpty()) {
                continue; // no tiene disponibilidad -> no se agrega
            }

            result.add(dto);
        }

        return result;
    }


    /**
     * Consulta la disponibilidad detallada de un docente específico para un semestre.
     */
    public GlobalAvabilityDTO getAvailabilityDTO(Long userId, Long semesterId, Long docentId ) {
        validateAdminAccess(userId);
        User docente = getUserById(docentId);
        Semester semester = getSemesterById(semesterId);
        List<Availability> availabilityList = availabilityRepository.findByUserAndSemester(docente, semester)
                .orElse(Collections.emptyList());

        return buildGlobalAvailabilityDTO(docente.getId(), availabilityList);
    }

    /**
     * Obtiene la disponibilidad global de todos los docentes para un semestre dado.
     */
    public List<GlobalAvabilityDTO> getListGlobalAvailability(Long semesterId, Long userId) {
        validateAdminAccess(userId);
        Semester semester = getSemesterById(semesterId);
        List<Availability> availabilityList = availabilityRepository.findBySemester(semester)
                .orElse(Collections.emptyList());

        return availabilityList.stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()))
                .entrySet().stream()
                .map(entry -> buildGlobalAvailabilityDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Consulta la disponibilidad de un docente en un semestre.
     */
    public AvailabilityDTO getAvailabilityByIdDocent(Long userId, Long semesterId) {
        User user = getUserById(userId);
        Semester semester = getSemesterById(semesterId);

        List<Availability> availabilityList = availabilityRepository.findByUserAndSemester(user, semester)
                .orElse(Collections.emptyList());

        Map<DayOfWeekEnum, List<AvailabilityBlockDTO>> availabilityMap = availabilityList.stream()
                .collect(Collectors.groupingBy(
                        Availability::getDayOfWeek,
                        Collectors.mapping(a -> AvailabilityBlockDTO.builder()
                                .id(a.getId())
                                .hour(a.getStartTime().getHour())
                                .statusId(a.getStatusAvailability().getId())
                                .statusDescription(a.getStatusAvailability().getDescription())
                                .build(), Collectors.toList())
                ));

        return AvailabilityDTO.builder().disponibilidad(availabilityMap).build();
    }

    /**
     * Crea o actualiza bloques de disponibilidad para un docente.
     */
    @Transactional
    public Boolean createAvailability(Long userId, Long semesterId, AvailabilityDTO dto) {
        User user = getUserById(userId);
        Semester semester = getSemesterById(semesterId);
        List<Availability> currentAvailability = getCurrentAvailability(user, semester);
        Map<String, AvailabilityBlockDTO> incomingMap = buildIncomingAvailabilityMap(dto);
        StatusAvailability defaultStatus = getDefaultStatus();

        deleteObsoleteAvailability(currentAvailability, incomingMap, userId);
        saveNewAvailabilityBlocks(dto, user, semester, incomingMap, defaultStatus);

        return true;
    }

    private void validateAdminAccess(Long userId) {
        User user = getUserById(userId);
        if (!userRolService.hasAdminPrivileges(user)) {
            log.warn("Acceso denegado: userId={} no tiene privilegios de administrador", userId);
            throw new GenericAppException(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta solicitud");
        }
    }

    private GlobalAvabilityDTO buildGlobalAvailabilityDTO(Long userId, List<Availability> availabilityList) {
        List<Long> areaIds = userAreaRepository.findByUserId(userId).stream()
                .map(userArea -> userArea.getArea().getId())
                .toList();

        Map<DayOfWeekEnum, List<AvailabilityBlockDTO>> disponibilidadMap = availabilityList.stream()
                .collect(Collectors.groupingBy(
                        Availability::getDayOfWeek,
                        Collectors.mapping(a -> {
                            AvailabilityBlockDTO dto = new AvailabilityBlockDTO();
                            dto.setId(a.getId());
                            dto.setHour(a.getStartTime().getHour());
                            dto.setStatusId(a.getStatusAvailability().getId());
                            dto.setStatusDescription(a.getStatusAvailability().getDescription());
                            return dto;
                        }, Collectors.toList())
                ));

        GlobalAvabilityDTO dto = new GlobalAvabilityDTO();
        dto.setUserIdDocent(userId);
        dto.setAreas(areaIds);
        dto.setDisponibilidad(disponibilidadMap);
        return dto;
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id={}", userId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
    }

    @Override
    public Semester getSemesterById(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });
    }

    @Override
    public StatusAvailability getDefaultStatus() {
        return statusAvailabilityRepository.findById(1L)
                .orElseThrow(() -> {
                    log.error("Estado de disponibilidad por defecto (id=1) no está configurado en base de datos");
                    return new GenericAppException(HttpStatus.NOT_FOUND,
                            "Estado de disponibilidad por defecto no está configurado");
                });
    }

    @Override
    public List<Availability> getCurrentAvailability(User user, Semester semester) {
        return availabilityRepository.findByUserAndSemester(user, semester)
                .orElse(new ArrayList<>());
    }

    @Override
    public Map<String, AvailabilityBlockDTO> buildIncomingAvailabilityMap(AvailabilityDTO dto) {
        return dto.getDisponibilidad().entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(block -> Map.entry(e.getKey() + "-" + block.getHour(), block)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Transactional
    public void deleteObsoleteAvailability(List<Availability> currentAvailability,
                                           Map<String, AvailabilityBlockDTO> incomingMap, Long userRequestId) {
        User userRequest = getUserById(userRequestId);
        Program currentProgram = getCurrentProgram();
        List<UserRol> userRolesRequest = userRoleRepository.findAllByUserAndProgram(userRequest, currentProgram)
                .orElse(new ArrayList<>());

        boolean isAdmin = userRolesRequest.stream().noneMatch(r -> r.getRole().getName().contains("PROFESOR"));

        for (Availability existing : currentAvailability) {
            String key = existing.getDayOfWeek() + "-" + existing.getStartTime().getHour();
            boolean existsInNew = incomingMap.containsKey(key);

            if (isAdmin || (!existsInNew &&
                    !existing.getStatusAvailability().getDescription().equalsIgnoreCase("APROBADO") &&
                    !existing.getStatusAvailability().getDescription().equalsIgnoreCase("RECHAZADO"))) {
                availabilityRepository.delete(existing);
            }
        }
    }

    @Override
    @Transactional
    public void saveNewAvailabilityBlocks(AvailabilityDTO dto, User user, Semester semester,
                                          Map<String, AvailabilityBlockDTO> incomingMap,
                                          StatusAvailability defaultStatus) {

        for (Map.Entry<DayOfWeekEnum, List<AvailabilityBlockDTO>> entry : dto.getDisponibilidad().entrySet()) {
            DayOfWeekEnum day = entry.getKey();

            for (AvailabilityBlockDTO block : entry.getValue()) {
                LocalTime time = LocalTime.of(block.getHour(), 0);

                if (availabilityRepository.existsByUserAndSemesterAndDayOfWeekAndStartTime(user, semester, day, time)) {
                    continue;
                }

                Availability availability = new Availability();
                availability.setUser(user);
                availability.setSemester(semester);
                availability.setDayOfWeek(day);
                availability.setStartTime(time);

                StatusAvailability status = (block.getStatusId() != null)
                        ? statusAvailabilityRepository.findById(block.getStatusId())
                        .orElseThrow(() -> {
                            log.warn("Estado de disponibilidad no encontrado con id={}", block.getStatusId());
                            return new GenericAppException(HttpStatus.NOT_FOUND, "Estado de disponibilidad no encontrado");
                        })
                        : defaultStatus;

                availability.setStatusAvailability(status);
                availabilityRepository.save(availability);
            }
        }
    }
    @Transactional
    public Boolean updateAvailabilityStatus(Long availabilityId, Long newStatusId) {
        // Buscar el bloque de disponibilidad por ID
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> {
                    log.warn("Disponibilidad no encontrada con id={}", availabilityId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Disponibilidad no encontrada");
                });

        StatusAvailability newStatus = statusAvailabilityRepository.findById(newStatusId)
                .orElseThrow(() -> {
                    log.warn("Estado de disponibilidad no encontrado con id={}", newStatusId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Estado de disponibilidad no encontrado");
                });

        // Actualizar el estado y guardar
        availability.setStatusAvailability(newStatus);
        availabilityRepository.save(availability);
        return true;
    }
    
    private Program getCurrentProgram() {
        return programRepository.findByCode(currentSchema)
                .orElseThrow(() -> {
                    log.error("Programa no encontrado para el schema={}", currentSchema);
                    return new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error de configuración: programa no encontrado");
                });
    }

}
