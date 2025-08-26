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
        Group group = new Group();
        if(dto.getIdDocente() != null) {
            User user2 = userRepository.findById(dto.getIdDocente())
                    .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            User user = getUserById(dto.getIdDocente()) ;
            group.setDocente(user);
        }

        group.setCode(dto.getCode());
        group.setSemester(semester);
        group.setSubject(subject);
        group.setMax_students(dto.getMax_students());
        group.setEnrolled(dto.getEnrolled());


        Group savedGroup = groupRepository.save(group);
        ScheduleCreateDTO scheduleCreateDTO = new ScheduleCreateDTO(
                savedGroup.getId(),
                dto.getIdDocente() != null ? savedGroup.getDocente().getId() : null,
                dto.getScheduleList()
        );
        scheduleService.createSchedule(scheduleCreateDTO,adminId);
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
