package judamov.sipoh.mappers;

import judamov.sipoh.dto.ScheduleDTO;
import judamov.sipoh.entity.Schedule;

import java.util.List;

public class ScheduleMapper {

    public static ScheduleDTO toDTO(Schedule schedule) {
        return new ScheduleDTO(
                schedule.getId(),
                schedule.getStartTime().getHour(),
                schedule.getDay()
        );
    }

    public static List<ScheduleDTO> toDTOList(List<Schedule> schedules) {
        return schedules.stream()
                .map(ScheduleMapper::toDTO)
                .toList();
    }
}
