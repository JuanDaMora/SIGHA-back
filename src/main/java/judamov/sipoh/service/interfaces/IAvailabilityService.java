package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.AvailabilityBlockDTO;
import judamov.sipoh.dto.AvailabilityDTO;
import judamov.sipoh.dto.GlobalAvabilityDTO;
import judamov.sipoh.entity.Availability;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.StatusAvailability;
import judamov.sipoh.entity.User;

import java.util.List;
import java.util.Map;

public interface IAvailabilityService {
    GlobalAvabilityDTO getAvailabilityDTO (Long userId, Long docentId, Long semesterId);
    List<GlobalAvabilityDTO> getListGlobalAvailability(Long semesterId, Long userId);
    AvailabilityDTO getAvailabilityByIdDocent(Long userId, Long semesterId);
    Boolean createAvailability(Long userId, Long semesterId, AvailabilityDTO dto);
    User getUserById(Long userId);
    Semester getSemesterById(Long semesterId);
    StatusAvailability getDefaultStatus();
    List<Availability> getCurrentAvailability(User user, Semester semester);
    Map<String, AvailabilityBlockDTO> buildIncomingAvailabilityMap(AvailabilityDTO dto);
    void deleteObsoleteAvailability(List<Availability> currentAvailability,
                                    Map<String, AvailabilityBlockDTO> incomingMap, Long userRequestId);
    void saveNewAvailabilityBlocks(AvailabilityDTO dto, User user, Semester semester,
                                   Map<String, AvailabilityBlockDTO> incomingMap,
                                   StatusAvailability defaultStatus);
    List<GlobalAvabilityDTO> getGlobalAvailabilityBySubjects(Long semesterId,
                                                                    Long userId,
                                                                    List<Long> subjectIds);
}
