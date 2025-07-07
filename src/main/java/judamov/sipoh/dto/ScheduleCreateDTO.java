package judamov.sipoh.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCreateDTO {
    private Long idGroup;
    private Long idDocente;
    private List<ScheduleDTO> scheduleList;
}
