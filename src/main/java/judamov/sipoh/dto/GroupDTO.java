package judamov.sipoh.dto;

import judamov.sipoh.entity.Schedule;
import judamov.sipoh.enums.DayOfWeekEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDTO {
    private Long id;
    private Long idSemestre;
    private Long idSubject;
    private Long idDocente;
    private Long idLevel;
    private String levelName;
    private String code;
    private List<ScheduleDTO> scheduleList;
}
