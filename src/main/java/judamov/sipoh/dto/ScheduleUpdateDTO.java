package judamov.sipoh.dto;

import judamov.sipoh.enums.DayOfWeekEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateDTO {
    private Long id; // ID del horario a editar
    private Integer hour;
    private DayOfWeekEnum day;
}
