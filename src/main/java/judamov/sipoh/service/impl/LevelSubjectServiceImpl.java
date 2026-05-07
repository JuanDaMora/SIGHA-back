package judamov.sipoh.service.impl;

import judamov.sipoh.dto.LevelSubjectDTO;
import judamov.sipoh.entity.LevelSubject;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.mappers.LevelSubjectMapper;
import judamov.sipoh.repository.ILevelSubjectRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.interfaces.ILevelSubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelSubjectServiceImpl implements ILevelSubjectService {

    private final IUserRepository userRepository;
    private final UserRolServiceImpl userRolService;
    private final ILevelSubjectRepository levelSubjectRepository;

    /**
     * Obtiene todos los levels existentes
     *
     * @param adminId  ID del usuario administrador que realiza la solicitud.
     * @return Lista de levels convertidos en DTO.
     */
    @Override
    public List<LevelSubjectDTO> getAll(Long adminId) {
        validateAdminAccess(adminId);
        List<LevelSubject> levelSubjectList= levelSubjectRepository.findAll();
        return LevelSubjectMapper.toDTOList(levelSubjectList);
    }

    /**
     * Verifica que un usuario tenga privilegios de administrador.
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
     * Obtiene un usuario por su ID o lanza una excepción si no existe.
     *
     * @param userId ID del usuario.
     * @return Entidad User encontrada.
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id={}", userId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
    }
}
