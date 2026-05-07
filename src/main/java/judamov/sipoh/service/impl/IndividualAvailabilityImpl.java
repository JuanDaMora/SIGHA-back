package judamov.sipoh.service.impl;

import judamov.sipoh.dto.IndividualAvailabilityDTO;
import judamov.sipoh.entity.IndividualAvailability;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.User;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IIndividualAvailabilityRepository;
import judamov.sipoh.repository.ISemesterRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.service.interfaces.IIndividualAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndividualAvailabilityImpl implements IIndividualAvailabilityService {

    private final IUserRepository userRepository;
    private final UserRolServiceImpl userRolService;
    private final ISemesterRepository semesterRepository;
    private final IIndividualAvailabilityRepository individualAvailabilityRepository;

    @Override
    public IndividualAvailabilityDTO upsertIndividualAvailability(Long semesterId, Long adminId, boolean isActive, Long docenteId) {
        validateAdminAccess(adminId);
        User docente = getValidatedTeacher(docenteId);
        Semester semester = getSemester(semesterId);

        IndividualAvailability availability = findUniqueAvailability(semester, docente);

        if (availability == null) {
            // No existe → lo creamos
            availability = IndividualAvailability.builder()
                    .user(docente)
                    .semester(semester)
                    .isActive(isActive)
                    .build();
        } else {
            // Ya existe → actualizamos si es necesario
            if (!availability.getIsActive().equals(isActive)) {
                availability.setIsActive(isActive);
            }
        }

        saveAvailability(availability, docenteId, semesterId);

        return toDTO(availability);
    }

    @Override
    public IndividualAvailabilityDTO getStatusIndividualAvailability(Long semesterId, Long adminId, Long docenteId) {
        validateAdminAccess(adminId);
        User docente = getUserById(docenteId);
        Semester semester = getSemester(semesterId);

        IndividualAvailability availability = findUniqueAvailability(semester, docente);
        if (availability == null) {
            log.warn("No existe disponibilidad individual para docenteId={} en semesterId={}", docenteId, semesterId);
            throw new GenericAppException(HttpStatus.NOT_FOUND,
                    "No existe disponibilidad individual para el docente en el semestre indicado");
        }

        return toDTO(availability);
    }

    // ------------------- Métodos públicos auxiliares -------------------

    /** Obtener usuario sin validar rol docente (usado en controller) */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con id={}", userId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
    }

    /** Obtener semestre por id (usado en controller) */
    public Semester getSemester(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> {
                    log.warn("Semestre no encontrado con id={}", semesterId);
                    return new GenericAppException(HttpStatus.NOT_FOUND, "Semestre no encontrado");
                });
    }

    // ------------------- Métodos privados -------------------

    private User getValidatedTeacher(Long userId) {
        User user = getUserById(userId);
        if (!userRolService.hasTeacherPrivileges(user)) {
            log.warn("userId={} no tiene rol de docente", userId);
            throw new GenericAppException(HttpStatus.FORBIDDEN, "El usuario no tiene rol de docente");
        }
        return user;
    }

    private void validateAdminAccess(Long adminId) {
        User admin = getUserById(adminId);
        if (!userRolService.hasAdminPrivileges(admin)) {
            log.warn("Acceso denegado: userId={} no tiene privilegios de administrador", adminId);
            throw new GenericAppException(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta solicitud");
        }
    }

    private IndividualAvailability findUniqueAvailability(Semester semester, User user) {
        List<IndividualAvailability> list = individualAvailabilityRepository.findBySemesterAndUser(semester, user);

        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            log.error("Inconsistencia de datos: userId={} tiene {} registros de disponibilidad individual en semesterId={}",
                    user.getId(), list.size(), semester.getId());
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error de consistencia en la disponibilidad individual del docente");
        }
        return list.get(0);
    }

    private void saveAvailability(IndividualAvailability availability, Long docenteId, Long semesterId) {
        try {
            individualAvailabilityRepository.save(availability);
        } catch (Exception e) {
            log.error("Error al guardar disponibilidad individual para docenteId={} en semesterId={}", docenteId, semesterId, e);
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al guardar la disponibilidad individual del docente");
        }
    }

    private IndividualAvailabilityDTO toDTO(IndividualAvailability availability) {
        return IndividualAvailabilityDTO.builder()
                .id(availability.getId())
                .userId(availability.getUser().getId())
                .semesterId(availability.getSemester().getId())
                .isActive(availability.getIsActive())
                .build();
    }
}
