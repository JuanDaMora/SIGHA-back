package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.SubjectCreateDTO;
import judamov.sipoh.entity.Area;
import judamov.sipoh.entity.LevelSubject;
import judamov.sipoh.entity.Subject;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IAreaRepository;
import judamov.sipoh.repository.ILevelSubjectRepository;
import judamov.sipoh.repository.ISubjectRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.interfaces.ISubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements ISubjectService {

    private final IUserRepository userRepository;
    private final ISubjectRepository subjectRepository;
    private final UserRolServiceImpl userRolService;
    private final IAreaRepository areaRepository;
    private final ILevelSubjectRepository levelSubjectRepository;


    @Override
    @Transactional
    public Boolean createSubject(SubjectCreateDTO subjectCreateDTO, Long adminId) {
        validateAdminAccess(adminId);

        Area area= areaRepository.findById(subjectCreateDTO.getIdArea())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Area no encontrada"));
        LevelSubject levelSubject= levelSubjectRepository.findById(subjectCreateDTO.getIdLevel())
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Nivel subject no encontrado"));
        Subject newSubject= Subject.builder()
                .codigo(subjectCreateDTO.getCode())
                .levelSubject(levelSubject)
                .area(area)
                .max_students(subjectCreateDTO.getMaxStudents())
                .name(subjectCreateDTO.getName())
                .build();
        try{
            subjectRepository.save(newSubject);
        }catch (Exception e){
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creando la asignatura");
        }
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

}
