package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.IndividualAvailabilityDTO;
import judamov.sipoh.entity.IndividualAvailability;

public interface IIndividualAvailabilityService {

    IndividualAvailabilityDTO getStatusIndividualAvailability(Long semesterId, Long userId, Long docenteId);

    IndividualAvailabilityDTO upsertIndividualAvailability(Long semesterId, Long adminId, boolean isActive, Long docenteId);
}
