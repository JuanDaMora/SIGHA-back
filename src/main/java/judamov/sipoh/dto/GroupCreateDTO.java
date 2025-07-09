package judamov.sipoh.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.util.List;

@Data
public class GroupCreateDTO {
    private String code;
    private Long idSubject;
    private Long idDocente;
    private String max_students;
    private String enrolled;
    private List<ScheduleDTO> scheduleList;
}
