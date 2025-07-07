package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.ScheduleCreateDTO;
import judamov.sipoh.dto.ScheduleDTO;

import java.util.List;

public interface IScheduleService {
    List<ScheduleDTO> createSchedule(ScheduleCreateDTO dto, Long adminId);
}
