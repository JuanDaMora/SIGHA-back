package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.ScheduleCreateDTO;
import judamov.sipoh.dto.ScheduleDTO;
import judamov.sipoh.entity.Group;
import judamov.sipoh.entity.Schedule;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IGroupRepository;
import judamov.sipoh.repository.IScheduleRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.interfaces.IScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements IScheduleService {
    private final IScheduleRepository scheduleRepository;
    private final UserRolServiceImpl userRolService;
    private final IUserRepository userRepository;
    private final IGroupRepository groupRepository;
    @Override
    @Transactional
    public List<ScheduleDTO> createSchedule(ScheduleCreateDTO dto, Long adminId) {
        validateAdminAccess(adminId);

        Group group = groupRepository.findById(dto.getIdGroup())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));

        // Borrar horarios previos del grupo
        scheduleRepository.findByGroup(group)
                .ifPresent(scheduleRepository::deleteAll);

        // Crear y guardar los nuevos horarios
        List<Schedule> schedulesToSave = dto.getScheduleList().stream()
                .map(s -> {
                    Schedule schedule = new Schedule();
                    schedule.setGroup(group);
                    schedule.setDay(s.getDay());
                    schedule.setStartTime(java.time.LocalTime.of(s.getHour(), 0));
                    return schedule;
                }).toList();

        List<Schedule> savedSchedules = scheduleRepository.saveAll(schedulesToSave);

        // Devolver la lista con IDs ya persistidos
        return savedSchedules.stream()
                .map(s -> new ScheduleDTO(s.getId(), s.getStartTime().getHour(), s.getDay()))
                .toList();
    }

    @Transactional
    public Boolean updateScheduleGroup(List<ScheduleCreateDTO> scheduleUpdateDTOS, Long adminID) {
        scheduleUpdateDTOS.forEach(scheduleCreateDTO -> createSchedule(scheduleCreateDTO, adminID));
        return true;
    }


    @Transactional
    public void deleteSceduleByGroup(Group group, Long adminid){
        validateAdminAccess(adminid);
        scheduleRepository.findByGroup(group)
                .ifPresent(scheduleRepository::deleteAll);
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

}
