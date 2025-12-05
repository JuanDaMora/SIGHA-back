package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.mappers.GroupMapper;
import judamov.sipoh.mappers.ScheduleMapper;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.interfaces.IGroupService;
import judamov.sipoh.service.interfaces.IScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

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

    /**
     * Válida que el docente no tenga ya otro grupo en los mismos bloques horarios
     * dentro del mismo semestre.
     */
    private void validateDocenteScheduleConflict(
            User docente,
            Semester semester,
            List<ScheduleDTO> scheduleList
    ) {
        if (docente == null || scheduleList == null || scheduleList.isEmpty()) {
            return;
        }

        List<String> conflicts = new java.util.ArrayList<>();

        for (ScheduleDTO block : scheduleList) {
            LocalTime startTime = LocalTime.of(block.getHour(), 0);

            boolean exists = scheduleRepository
                    .existsByGroupDocenteAndGroupSemesterAndDayAndStartTime(
                            docente,
                            semester,
                            block.getDay(),
                            startTime
                    );

            if (exists) {
                conflicts.add(
                        String.format("%s %02d:00", block.getDay().name(), block.getHour())
                );
            }
        }

        // Si hubo conflictos, lanzar UNA SOLA excepción con todos
        if (!conflicts.isEmpty()) {
            String joined = String.join("\n- ", conflicts);
            String msg = "El docente tiene conflictos de horario:\n- " + joined;
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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "El semestre con id " + semesterId + " no existe"));

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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "El semestre con id "+semesterId+" no existe"));

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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada"));
        Semester semester= semesterRepository.findById(semesterId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "El semestre con id "+semesterId+" no existe"));

        List<Group> groups = groupRepository.findBySubjectAndSemester(subject,semester)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "No hay grupos para esta materia"));

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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "El semestre con id "+semesterId+" no existe"));

        List<Group> groups = groupRepository.findByDocenteAndSemester(docente,semester)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "El docente no tiene grupos asignados"));

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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado"));

        User docente = null;
        if (dto.getIdDocente() != null) {
            docente = userRepository.findById(dto.getIdDocente())
                    .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        }
        //Validar conflicto de horarios ANTES de crear el grupo y los schedules
        validateDocenteScheduleConflict(docente, semester, dto.getScheduleList());
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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));

        try {
            if (idDocente == null) {
                // Quitar el docente del grupo si no se proporciona
                group.setDocente(null);
            } else {
                // Asignar nuevo docente
                User newDocente = userRepository.findById(idDocente)
                        .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Docente no encontrado"));
                group.setDocente(newDocente);
            }

            groupRepository.save(group);
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar el docente en el grupo " + group.getCode());
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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));

        Subject subject = subjectRepository.findById(dto.getIdSubject())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado"));

        User docente = (dto.getIdDocente() != null) ? getUserById(dto.getIdDocente()) : null;


        if(!dto.getScheduleList().isEmpty()){
            ScheduleCreateDTO scheduleCreateDTO=ScheduleCreateDTO.builder()
                    .idGroup(groupId)
                    .scheduleList(dto.getScheduleList())
                    .build();
            scheduleService.createSchedule(scheduleCreateDTO,adminId);
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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));
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
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para esta solicitud");
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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
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
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado"));

        List<Group> groupList = groupRepository.findBySemester(semester);
        if (groupList.isEmpty()) {
            throw new GenericAppException(HttpStatus.NOT_FOUND, "No se encontraron grupos en el semestre");
        } else {

            // Primero eliminar schedules
            scheduleRepository.deleteByGroupIn(groupList);

            // Después eliminar los grupos
            groupRepository.deleteAll(groupList);
        }



        return true;
    }

}
