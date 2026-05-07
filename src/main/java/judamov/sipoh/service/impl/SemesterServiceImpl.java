package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.SemesterDTO;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.ISemesterRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.repository.IUserRoleRepository;
import judamov.sipoh.service.interfaces.ISemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements ISemesterService {
    private final ISemesterRepository semesterRepository;
    private final IUserRepository userRepository;
    private final UserRolServiceImpl userRolService;
    @Override
    public List<Semester> getAllSemesters(){
        return semesterRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
    @Override
    @Transactional
    public Boolean addSemester(SemesterDTO semesterDTO){
        semesterRepository.findOneByDescription(semesterDTO.getDescription())
                .ifPresent(semester -> {
                    throw new GenericAppException(HttpStatus.CONFLICT,
                            "Ya existe un semestre con la descripción: " + semesterDTO.getDescription());
                });
        Semester newSemester = new Semester(semesterDTO);
        try{
            semesterRepository.save(newSemester);
        }catch(Exception e){
            log.error("Error al guardar semestre con descripción='{}'", semesterDTO.getDescription(), e);
            throw new GenericAppException(HttpStatus.BAD_REQUEST,
                    "Error al guardar el semestre. Verifique los datos enviados.");
        }
        return true;
    }
    @Override
    @Transactional
    public SemesterDTO updateSemester(SemesterDTO semesterDTO){
        Semester semester= semesterRepository.findOneById(semesterDTO.getId())
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterDTO.getId());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });

        semester.updateFromDto(semesterDTO);
        Semester saved = semesterRepository.save(semester);

        return new SemesterDTO(saved);

    }
    @Override
    @Transactional
    public Boolean changeAvailability(Boolean newAvailability,Long semesterId,Long userId){
        validateAdminAccess(userId);
        Semester semester= semesterRepository.findOneById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });
        if(semester.getAvailability()!=newAvailability){
            semester.setAvailability(newAvailability);
            semesterRepository.save(semester);
        }
        return true;
    }
    private void validateAdminAccess(Long userId) {
        User user = getUserById(userId);
        if (!userRolService.hasAdminPrivileges(user)) {
            log.warn("Acceso denegado: userId={} no tiene privilegios de administrador", userId);
            throw new GenericAppException(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta solicitud");
        }
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id={}", userId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
    }
}
