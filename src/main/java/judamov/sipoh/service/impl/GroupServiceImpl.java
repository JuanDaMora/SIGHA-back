package judamov.sipoh.service.impl;

import judamov.sipoh.dto.*;
import judamov.sipoh.entity.*;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.mappers.GroupMapper;
import judamov.sipoh.mappers.ScheduleMapper;
import judamov.sipoh.repository.*;
import judamov.sipoh.service.interfaces.IGroupService;
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
    private final ScheduleServiceImpl scheduleService;

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
     * @param idSubject ID de la materia.
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
     * @param idUser  ID del docente.
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
    public Boolean createGroup(GroupCreateDTO dto, Long adminId, Long semesterId) {
        validateAdminAccess(adminId);

        Subject subject = subjectRepository.findById(dto.getIdSubject())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado"));

        User user = (dto.getIdDocente() != null) ? getUserById(dto.getIdDocente()) : null;

        Group group = new Group();
        group.setCode(dto.getCode());
        group.setSemester(semester);
        group.setSubject(subject);
        group.setDocente(user);

        Group savedGroup = groupRepository.save(group);
        ScheduleCreateDTO scheduleCreateDTO = new ScheduleCreateDTO(
                savedGroup.getId(),
                dto.getScheduleList()
        );
        scheduleService.createSchedule(scheduleCreateDTO,adminId);
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

        User user = (dto.getIdDocente() != null) ? getUserById(dto.getIdDocente()) : null;

        group.setCode(dto.getCode());
        group.setSemester(semester);
        group.setSubject(subject);
        group.setDocente(user);

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


}
