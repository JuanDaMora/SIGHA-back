package judamov.sipoh.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupCreateDTO {
    private String code;
    private Long idSubject;
    private Long idDocente;
    private List<ScheduleDTO> scheduleList;
}
