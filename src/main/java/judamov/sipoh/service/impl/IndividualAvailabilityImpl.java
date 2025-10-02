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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

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

        saveAvailability(availability, "Error al guardar la disponibilidad individual");

        return toDTO(availability);
    }

    @Override
    public IndividualAvailabilityDTO getStatusIndividualAvailability(Long semesterId, Long adminId, Long docenteId) {
        validateAdminAccess(adminId);
        User docente = getUserById(docenteId);
        Semester semester = getSemester(semesterId);

        IndividualAvailability availability = findUniqueAvailability(semester, docente);
        if (availability == null) {
            throw new GenericAppException(HttpStatus.NOT_FOUND, "No existe disponibilidad individual");
        }

        return toDTO(availability);
    }

    // ------------------- Métodos públicos auxiliares -------------------

    /** Obtener usuario sin validar rol docente (usado en controller) */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    /** Obtener semestre por id (usado en controller) */
    public Semester getSemester(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new GenericAppException(HttpStatus.NOT_FOUND,
                        "El semestre con id " + semesterId + " no existe"));
    }

    // ------------------- Métodos privados -------------------

    private User getValidatedTeacher(Long userId) {
        User user = getUserById(userId);
        if (!userRolService.hasTeacherPrivileges(user)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "El usuario no es un docente");
        }
        return user;
    }

    private void validateAdminAccess(Long adminId) {
        User admin = getUserById(adminId);
        if (!userRolService.hasAdminPrivileges(admin)) {
            throw new GenericAppException(HttpStatus.UNAUTHORIZED, "No autorizado para esta solicitud");
        }
    }

    private IndividualAvailability findUniqueAvailability(Semester semester, User user) {
        List<IndividualAvailability> list = individualAvailabilityRepository.findBySemesterAndUser(semester, user);

        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "El usuario tiene más de una disponibilidad individual");
        }
        return list.get(0);
    }

    private void saveAvailability(IndividualAvailability availability, String errorMessage) {
        try {
            individualAvailabilityRepository.save(availability);
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
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
