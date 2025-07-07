package judamov.sipoh.dto;

import judamov.sipoh.enums.DayOfWeekEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private Long id;
    private Integer hour;
    private DayOfWeekEnum day;
}
