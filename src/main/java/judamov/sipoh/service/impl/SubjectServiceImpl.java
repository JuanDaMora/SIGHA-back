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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
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
                .orElseThrow(() -> {
                    log.warn("Área no encontrada con id={}", subjectCreateDTO.getIdArea());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Área no encontrada");
                });
        LevelSubject levelSubject= levelSubjectRepository.findById(subjectCreateDTO.getIdLevel())
                .orElseThrow(() -> {
                    log.warn("Nivel académico no encontrado con id={}", subjectCreateDTO.getIdLevel());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Nivel académico no encontrado");
                });
        Subject newSubject= Subject.builder()
                .codigo(subjectCreateDTO.getCode())
                .levelSubject(levelSubject)
                .area(area)
                .name(subjectCreateDTO.getName())
                .build();
        try{
            subjectRepository.save(newSubject);
        }catch (Exception e){
            log.error("Error al crear asignatura nombre='{}'", subjectCreateDTO.getName(), e);
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear la asignatura");
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
    @Override
    @Transactional
    public Boolean updateSubject(Long subjectId, SubjectCreateDTO dto, Long adminId) {
        validateAdminAccess(adminId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> {
                    log.warn("Asignatura no encontrada con id={}", subjectId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Asignatura no encontrada");
                });

        Area area = areaRepository.findById(dto.getIdArea())
                .orElseThrow(() -> {
                    log.warn("Área no encontrada con id={}", dto.getIdArea());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Área no encontrada");
                });

        LevelSubject levelSubject = levelSubjectRepository.findById(dto.getIdLevel())
                .orElseThrow(() -> {
                    log.warn("Nivel académico no encontrado con id={}", dto.getIdLevel());
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Nivel académico no encontrado");
                });

        subject.setCodigo(dto.getCode());
        subject.setName(dto.getName());
        subject.setArea(area);
        subject.setLevelSubject(levelSubject);

        try {
            subjectRepository.save(subject);
        } catch (Exception e) {
            log.error("Error al actualizar asignatura id={}", subjectId, e);
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar la asignatura");
        }

        return true;
    }


}
