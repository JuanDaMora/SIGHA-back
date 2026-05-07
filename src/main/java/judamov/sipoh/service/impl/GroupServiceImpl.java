package judamov.sipoh.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.enums.DayOfWeekEnum;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.mappers.GroupMapper;
import judamov.sipoh.mappers.ScheduleMapper;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.interfaces.IGroupService;
import judamov.sipoh.service.interfaces.IScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements IGroupService {

    private final IGroupRepository groupRepository;
    private final IUserRepository userRepository;
    private final UserRolServiceImpl userRolService;
    private final ISubjectRepository subjectRepository;
    private final ISemesterRepository semesterRepository;
    private final IScheduleRepository scheduleRepository;
    private final IScheduleService scheduleService;
    private final EntityManager entityManager;

    /**
     * Válida que el docente no tenga ya otro grupo en los mismos bloques horarios
     * dentro del mismo semestre.
     */

    /**
     * Valida que el docente no tenga ya otros grupos en los mismos bloques
     * horarios dentro del semestre. Si excludeGroupId != null, ignora ese grupo
     * (para el caso de update).
     */
    private void validateDocenteScheduleConflict(
            User docente,
            Semester semester,
            List<ScheduleDTO> scheduleList,
            Long excludeGroupId
    ) {
        if (docente == null || scheduleList == null || scheduleList.isEmpty()) {
            return;
        }

        List<String> conflicts = new ArrayList<>();

        for (ScheduleDTO block : scheduleList) {
            LocalTime startTime = LocalTime.of(block.getHour(), 0);

            boolean exists;

            if (excludeGroupId == null) {
                // CREATE
                exists = scheduleRepository
                        .existsByGroupDocenteAndGroupSemesterAndDayAndStartTime(
                                docente,
                                semester,
                                block.getDay(),
                                startTime
                        );
            } else {
                // UPDATE → ignorar el mismo grupo
                exists = scheduleRepository
                        .existsByGroupDocenteAndGroupSemesterAndDayAndStartTimeAndGroupIdNot(
                                docente,
                                semester,
                                block.getDay(),
                                startTime,
                                excludeGroupId
                        );
            }

            if (exists) {
                conflicts.add(
                        String.format("%s %02d:00", block.getDay().name(), block.getHour())
                );
            }
        }

        if (!conflicts.isEmpty()) {

            String docenteNombre = docente.getFirstName() + " " + docente.getLastName();
            String joined = String.join("\n- ", conflicts);

            String msg = String.format(
                    "El docente %s tiene conflictos de horario:\n- %s",
                    docenteNombre,
                    joined
            );
            throw new GenericAppException(HttpStatus.CONFLICT, msg);
        }
    }


    @Override
    public List<GroupDTO> getAllByFilters(List<Long> idLevels,
                                          List<Long> docentesIds,
                                          List<Long> subjectIds,
                                          Long adminId,
                                          Long semesterId) {

        validateAdminAccess(adminId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        // 1. Traer todos los grupos del semestre
        List<Group> groups = groupRepository.findBySemester(semester);

        // 2. Si NO hay filtros → devolver todos los grupos del semestre
        boolean noLevelFilter   = (idLevels == null || idLevels.isEmpty());
        boolean noDocenteFilter = (docentesIds == null || docentesIds.isEmpty());
        boolean noSubjectFilter = (subjectIds == null || subjectIds.isEmpty());

        if (noLevelFilter && noDocenteFilter && noSubjectFilter) {
            return mapWithSchedules(groups);
        }

        // 3. Aplicar filtros SOLO a los criterios enviados
        List<Group> filtered = groups.stream()
                .filter(group -> {

                    // Filtro nivel
                    boolean matchesLevel = noLevelFilter ||
                            (group.getSubject() != null &&
                                    group.getSubject().getLevelSubject() != null &&
                                    idLevels.contains(group.getSubject().getLevelSubject().getId()));

                    // Filtro docente
                    boolean matchesDocente = noDocenteFilter ||
                            (group.getDocente() != null &&
                                    docentesIds.contains(group.getDocente().getId()));

                    // Filtro asignatura
                    boolean matchesSubject = noSubjectFilter ||
                            (group.getSubject() != null &&
                                    subjectIds.contains(group.getSubject().getId()));

                    return matchesLevel && matchesDocente && matchesSubject;
                })
                .toList();

        // No lanzar error si no hay resultados: depende de tu front.
        return mapWithSchedules(filtered);
    }


    /**
     * Obtiene todos los grupos de un semestre específico.
     *
     * @param semesterId ID del semestre.
     * @param adminId    ID del administrador que hace la consulta.
     * @return Lista de DTOs con horarios incluidos.
     */
    @Override
    public List<GroupDTO> getAllBySemester(Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        List<Group> groups = groupRepository.findAll().stream()
                .filter(group -> group.getSemester() != null && group.getSemester().getId().equals(semesterId))
                .toList();

        return mapWithSchedules(groups);
    }

    @Override
    public List<GroupDTO> getAllBySemesterAllPrograms(Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        List<Object[]> rows = groupRepository.findAllProgramsBySemester(semesterId);
        List<GroupDTO> all = rows.stream()
                .map(this::mapRowToGroupDTO)
                .toList();

        // Cargar horarios para todos los grupos
        loadSchedulesForDTOs(all);

        // Filtrar grupos que no tienen horarios
        return all.stream()
                .filter(dto -> dto.getScheduleList() != null && !dto.getScheduleList().isEmpty())
                .toList();
    }

    @Override
    public List<GroupDTO> getAllByFiltersAllPrograms(List<Long> idLevels,
                                                     List<Long> docentesIds,
                                                     List<Long> subjectIds,
                                                     List<Long> programIds,
                                                     Long adminId,
                                                     Long semesterId) {
        validateAdminAccess(adminId);

        List<Object[]> rows = groupRepository.findAllProgramsBySemester(semesterId);
        List<GroupDTO> all = rows.stream()
                .map(this::mapRowToGroupDTO)
                .toList();

        // Cargar horarios para todos los grupos primero
        loadSchedulesForDTOs(all);

        // Si no hay ningún filtro, devolver todos los grupos (con horarios cargados)
        boolean hasLevelFilter = idLevels != null && !idLevels.isEmpty();
        boolean hasDocenteFilter = docentesIds != null && !docentesIds.isEmpty();
        boolean hasSubjectFilter = subjectIds != null && !subjectIds.isEmpty();
        boolean hasProgramFilter = programIds != null && !programIds.isEmpty();

        if (!hasLevelFilter && !hasDocenteFilter && !hasSubjectFilter && !hasProgramFilter) {
            // Filtrar grupos que no tienen horarios
            return all.stream()
                    .filter(dto -> dto.getScheduleList() != null && !dto.getScheduleList().isEmpty())
                    .toList();
        }

        // Aplicar filtros
        List<GroupDTO> filtered = all.stream()
                .filter(dto -> {
                    boolean matchesLevel = !hasLevelFilter ||
                            (dto.getIdLevel() != null && idLevels.contains(dto.getIdLevel()));

                    boolean matchesDocente = !hasDocenteFilter ||
                            (dto.getIdDocente() != null && docentesIds.contains(dto.getIdDocente()));

                    boolean matchesSubject = !hasSubjectFilter ||
                            (dto.getIdSubject() != null && subjectIds.contains(dto.getIdSubject()));

                    boolean matchesProgram = !hasProgramFilter ||
                            (dto.getProgramId() != null && programIds.contains(dto.getProgramId()));

                    return matchesLevel && matchesDocente && matchesSubject && matchesProgram;
                })
                .toList();

        // Filtrar grupos que no tienen horarios
        return filtered.stream()
                .filter(dto -> dto.getScheduleList() != null && !dto.getScheduleList().isEmpty())
                .toList();
    }

    /**
     * Obtiene los grupos cuyo nivel esté incluido en la lista de niveles.
     *
     * @param idLevels Lista de IDs de niveles.
     * @param adminId  ID del administrador que hace la consulta.
     * @return Lista de DTOs con horarios incluidos.
     */
    @Override
    public List<GroupDTO> getAllByLevels(List<Long> idLevels, Long adminId, Long semesterId) {
        validateAdminAccess(adminId);
        Semester semester= semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        List<Group> groups = groupRepository.findBySemester(semester).stream()
                .filter(group -> group.getSubject() != null &&
                        group.getSubject().getLevelSubject() != null &&
                        idLevels.contains(group.getSubject().getLevelSubject().getId()))
                .toList();

        return mapWithSchedules(groups);
    }

    /**
     * Obtiene todos los grupos de una materia específica.
     *
     * @param subjectId ID de la materia.
     * @param adminId   ID del administrador que hace la consulta.
     * @return Lista de DTOs con horarios incluidos.
     */
    @Override
    public List<GroupDTO> getAllBySubject(Long subjectId, Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> {
                    log.warn("Materia no encontrada con id={}", subjectId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada");
                });
        Semester semester= semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        List<Group> groups = groupRepository.findBySubjectAndSemester(subject,semester)
                .orElseThrow(() -> {
                    log.warn("No hay grupos para subjectId={} en semesterId={}", subjectId, semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "No hay grupos para la materia en el semestre indicado");
                });

        return mapWithSchedules(groups);
    }

    /**
     * Obtiene todos los grupos asignados a un docente específico.
     *
     * @param docenteId  ID del docente.
     * @param adminId ID del administrador que hace la consulta.
     * @return Lista de DTOs con horarios incluidos.
     */
    @Override
    public List<GroupDTO> getAllByDocente(Long docenteId, Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        User docente = getUserById(docenteId);
        Semester semester= semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        List<Group> groups = groupRepository.findByDocenteAndSemester(docente,semester)
                .orElseThrow(() -> {
                    log.warn("Docente id={} no tiene grupos en semesterId={}", docenteId, semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "El docente no tiene grupos asignados en el semestre indicado");
                });

        return mapWithSchedules(groups);
    }

    /**
     * Crea un nuevo grupo con base en los datos recibidos.
     *
     * @param dto     DTO con la información del grupo.
     * @param adminId ID del administrador que realiza la operación.
     * @return Grupo creado en forma de DTO.
     */
    @Override
    @Transactional
    public Boolean createGroup(GroupCreateDTO dto, Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        Subject subject = subjectRepository.findById(dto.getIdSubject())
                .orElseThrow(() -> {
                    log.warn("Materia no encontrada con id={}", dto.getIdSubject());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada");
                });

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        User docente = null;
        if (dto.getIdDocente() != null) {
            docente = userRepository.findById(dto.getIdDocente())
                    .orElseThrow(() -> {
                        log.warn("Docente no encontrado con id={}", dto.getIdDocente());
                        return new GenericAppException(HttpStatus.NOT_FOUND, "Docente no encontrado");
                    });
        }
        //Validar conflicto de horarios ANTES de crear el grupo y los schedules
        validateDocenteScheduleConflict(docente, semester, dto.getScheduleList(), null);

        Group group = new Group();
        group.setCode(dto.getCode());
        group.setSemester(semester);
        group.setSubject(subject);
        group.setDocente(docente);
        group.setMax_students(dto.getMax_students());
        group.setEnrolled(dto.getEnrolled());

        Group savedGroup = groupRepository.save(group);

        ScheduleCreateDTO scheduleCreateDTO = new ScheduleCreateDTO(
                savedGroup.getId(),
                docente != null ? docente.getId() : null,
                dto.getScheduleList()
        );
        scheduleService.createSchedule(scheduleCreateDTO, adminId);

        return true;
    }
    @Override
    public Boolean updateDocente(Long groupId,Long idDocente, Long adminId){
        validateAdminAccess(adminId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.warn("Grupo no encontrado con id={}", groupId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
                });

        try {
            if (idDocente == null) {
                group.setDocente(null);
            } else {
                User newDocente = userRepository.findById(idDocente)
                        .orElseThrow(() -> {
                            log.warn("Docente no encontrado con id={}", idDocente);
                            return new GenericAppException(HttpStatus.NOT_FOUND, "Docente no encontrado");
                        });
                group.setDocente(newDocente);
            }

            groupRepository.save(group);
        } catch (GenericAppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar docente en grupo id={} código={}", groupId, group.getCode(), e);
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar el grupo");
        }
        return true;
    }
    /**
     * Actualiza un grupo existente.
     *
     * @param groupId ID del grupo a actualizar.
     * @param dto     Datos nuevos del grupo.
     * @param adminId ID del administrador que realiza la operación.
     * @return Grupo actualizado en forma de DTO.
     */
    @Override
    public Boolean updateGroup(Long groupId, GroupUpdateDTO dto, Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.warn("Grupo no encontrado con id={}", groupId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
                });

        Subject subject = subjectRepository.findById(dto.getIdSubject())
                .orElseThrow(() -> {
                    log.warn("Materia no encontrada con id={}", dto.getIdSubject());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada");
                });

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        User docente = (dto.getIdDocente() != null) ? getUserById(dto.getIdDocente()) : null;

        // si vienen horarios, validar conflictos excluyendo este mismo grupo
        if (dto.getScheduleList() != null && !dto.getScheduleList().isEmpty()) {
            validateDocenteScheduleConflict(docente, semester, dto.getScheduleList(), groupId);

            ScheduleCreateDTO scheduleCreateDTO = ScheduleCreateDTO.builder()
                    .idGroup(groupId)
                    .scheduleList(dto.getScheduleList())
                    .build();
            scheduleService.createSchedule(scheduleCreateDTO, adminId);
        }

        group.setCode(dto.getCode());
        group.setSemester(semester);
        group.setSubject(subject);
        group.setDocente(docente);
        group.setMax_students(dto.getMax_students());
        group.setEnrolled(dto.getEnrolled());

        groupRepository.save(group);
        return true;
    }




    public Boolean deleteGroup(Long groupId, Long adminId){
        validateAdminAccess(adminId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.warn("Grupo no encontrado con id={}", groupId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
                });
        scheduleService.deleteSceduleByGroup(group,adminId);
        groupRepository.delete(group);
        return true;
    }

    /**
     * Verifica que el usuario tenga privilegios de administrador.
     *
     * @param userId ID del usuario a validar.
     */
    private void validateAdminAccess(Long userId) {
        User user = getUserById(userId);
        if (!userRolService.hasAdminPrivileges(user)) {
            log.warn("Acceso denegado: userId={} no tiene privilegios de administrador", userId);
            throw new GenericAppException(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta solicitud");
        }
    }

    /**
     * Obtiene un usuario por su ID o lanza excepción si no existe.
     *
     * @param userId ID del usuario.
     * @return Entidad User correspondiente.
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id={}", userId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
    }

    /**
     * Mapea una lista de entidades Group a GroupDTO, y les asigna su lista de horarios (Schedule).
     *
     * @param groups Lista de entidades Group.
     * @return Lista de DTOs con su lista de Schedule incluida.
     */
    private List<GroupDTO> mapWithSchedules(List<Group> groups) {
        List<GroupDTO> groupDTOList = GroupMapper.toDTOList(groups);

        groupDTOList.forEach(groupDTO -> {
            // Buscar el grupo correspondiente por ID
            Group group = groups.stream()
                    .filter(g -> g.getId().equals(groupDTO.getId()))
                    .findFirst()
                    .orElse(null);

            if (group != null) {
                // Obtener los horarios del grupo
                List<Schedule> schedules = scheduleRepository.findByGroup(group).orElse(List.of());
                List<ScheduleDTO> scheduleDTOs = ScheduleMapper.toDTOList(schedules);
                groupDTO.setScheduleList(scheduleDTOs);
            }

            // Si no se encontró el grupo o la lista vino como null, se asegura una lista vacía
            if (groupDTO.getScheduleList() == null) {
                groupDTO.setScheduleList(List.of());
            }
        });

        return groupDTOList;
    }

    /**
     * Mapea una fila devuelta por la vista core.v_all_groups_by_semester
     * a un GroupDTO, incluyendo los campos de programa/escuela.
     *
     * Orden esperado de las columnas (Object[] row):
     * 0: id
     * 1: id_semestre
     * 2: id_subject
     * 3: code_subject
     * 4: name_subject
     * 5: id_docente
     * 6: id_level
     * 7: level_name
     * 8: code
     * 9: max_students
     * 10: enrolled
     * 11: program_code
     * 12: program_name
     * 13: escuela
     */
    private GroupDTO mapRowToGroupDTO(Object[] row) {
        GroupDTO dto = new GroupDTO();

        dto.setId(getLong(row[0]));
        dto.setIdSemestre(getLong(row[1]));
        dto.setIdSubject(getLong(row[2]));
        dto.setCodeSubject((String) row[3]);
        dto.setNameSubject((String) row[4]);
        dto.setIdDocente(getLong(row[5]));
        dto.setIdLevel(getLong(row[6]));
        dto.setLevelName((String) row[7]);
        dto.setCode((String) row[8]);
        dto.setMax_students((String) row[9]);
        dto.setEnrolled((String) row[10]);
        dto.setProgramId(getLong(row[11]));
        dto.setProgramCode((String) row[12]);
        dto.setProgramName((String) row[13]);
        dto.setEscuela((String) row[14]);

        // La vista no incluye horarios; se cargarán después
        dto.setScheduleList(List.of());

        return dto;
    }

    /**
     * Carga los horarios para una lista de GroupDTOs desde sus schemas correspondientes.
     * Agrupa los grupos por schema para optimizar las consultas.
     *
     * @param groupDTOs Lista de DTOs a los que se les cargarán los horarios
     */
    private void loadSchedulesForDTOs(List<GroupDTO> groupDTOs) {
        if (groupDTOs == null || groupDTOs.isEmpty()) {
            return;
        }

        // Agrupar grupos por schema (programCode)
        Map<String, List<GroupDTO>> groupsBySchema = groupDTOs.stream()
                .filter(dto -> dto.getProgramCode() != null)
                .collect(Collectors.groupingBy(GroupDTO::getProgramCode));

        // Para cada schema, cargar horarios en batch
        groupsBySchema.forEach((programCode, dtos) -> {
            String schema = getSchemaFromProgramCode(programCode);
            if (schema != null) {
                List<Long> groupIds = dtos.stream()
                        .map(GroupDTO::getId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());

                if (!groupIds.isEmpty()) {
                    Map<Long, List<ScheduleDTO>> schedulesMap = getSchedulesFromSchema(groupIds, schema);
                    // Asignar horarios a cada DTO
                    dtos.forEach(dto -> {
                        List<ScheduleDTO> schedules = schedulesMap.getOrDefault(dto.getId(), List.of());
                        dto.setScheduleList(schedules);
                    });
                }
            }
        });
    }

    /**
     * Obtiene el nombre del schema a partir del código del programa.
     * Valida que el schema sea uno de los permitidos para seguridad.
     *
     * @param programCode Código del programa (ej: "ing_sistemas")
     * @return Nombre del schema (ej: "ing_sistemas") o null si no es válido
     */
    private String getSchemaFromProgramCode(String programCode) {
        if (programCode == null) {
            return null;
        }
        // Validar que el schema sea uno de los permitidos
        List<String> validSchemas = List.of(
                "ing_biomedica",
                "ing_ciencia_de_datos",
                "ing_inteligencia_artificial",
                "ing_sistemas"
        );
        return validSchemas.contains(programCode) ? programCode : null;
    }

    /**
     * Obtiene los horarios de múltiples grupos desde un schema específico usando consulta nativa.
     *
     * @param groupIds Lista de IDs de grupos
     * @param schema   Nombre del schema (ej: "ing_sistemas")
     * @return Mapa de groupId -> Lista de ScheduleDTO
     */
    private Map<Long, List<ScheduleDTO>> getSchedulesFromSchema(List<Long> groupIds, String schema) {
        if (groupIds == null || groupIds.isEmpty() || schema == null) {
            return Map.of();
        }

        // Construir la consulta nativa
        String sql = String.format(
                "SELECT id, id_group, EXTRACT(HOUR FROM start_time)::int AS hour, day_of_week " +
                "FROM %s.schedule " +
                "WHERE id_group IN (:groupIds)",
                schema
        );

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("groupIds", groupIds);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Mapear resultados a ScheduleDTO agrupados por id_group
        return results.stream()
                .collect(Collectors.groupingBy(
                        row -> getLong(row[1]), // id_group
                        Collectors.mapping(
                                row -> {
                                    Long id = getLong(row[0]);
                                    Integer hour = row[2] != null ? ((Number) row[2]).intValue() : null;
                                    String dayStr = (String) row[3];
                                    DayOfWeekEnum day = dayStr != null ? DayOfWeekEnum.valueOf(dayStr) : null;
                                    return new ScheduleDTO(id, hour, day);
                                },
                                Collectors.toList()
                        )
                ));
    }

    private Long getLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * Crea múltiples grupos en bloque.
     *
     * @param dtos       Lista de DTOs con la información de los grupos.
     * @param adminId    ID del administrador que realiza la operación.
     * @param semesterId ID del semestre al que pertenecen todos los grupos.
     * @return true si todos los grupos se crearon sin errores; lanza excepción si alguno falla.
     */
    @Override
    @Transactional
    public Boolean createGroupsBulk(List<GroupCreateDTO> dtos, Long adminId, Long semesterId) {
        // Verificar sólo una vez
        validateAdminAccess(adminId);

        for (GroupCreateDTO dto : dtos) {
            // Reutiliza tu método existente
            createGroup(dto, adminId, semesterId);
        }

        return true;
    }
    @Transactional
    @Override
    public Boolean deleteAllGroupsBySemesterId(Long userId, Long semesterId){
        validateAdminAccess(userId);
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "Semestre no encontrado con id: " + semesterId));

        List<Group> groupList = groupRepository.findBySemester(semester);
        if (groupList.isEmpty()) {
            throw new GenericAppException(HttpStatus.NOT_FOUND,
                    "No se encontraron grupos en el semestre con id: " + semesterId);
        } else {

            // Primero eliminar schedules
            scheduleRepository.deleteByGroupIn(groupList);

            // Después eliminar los grupos
            groupRepository.deleteAll(groupList);
        }



        return true;
    }

}
