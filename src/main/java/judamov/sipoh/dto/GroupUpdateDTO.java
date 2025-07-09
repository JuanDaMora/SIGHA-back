package judamov.sipoh.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupUpdateDTO {
    private String code;
    private Long idSubject;
    private Long idDocente;
    private String max_students;
    private String enrolled;
    private List<ScheduleDTO> scheduleList;
}
