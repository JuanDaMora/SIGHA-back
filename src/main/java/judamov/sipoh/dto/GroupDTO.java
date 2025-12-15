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
    private String codeSubject;
    private String nameSubject;
    private Long idDocente;
    private Long idLevel;
    private String levelName;
    private String code;
    private String max_students;
    private String enrolled;
    /**
     * Código interno del programa/escuela (por ejemplo: ING_SISTEMAS).
     * Se usa únicamente en el endpoint global que consulta la vista
     * multi-programa; en los endpoints actuales puede venir en null.
     */
    private String programCode;

    /**
     * Nombre legible del programa (por ejemplo: Ingeniería de Sistemas).
     */
    private String programName;

    /**
     * Nombre de la escuela/facultad a la que pertenece el programa.
     */
    private String escuela;

    private List<ScheduleDTO> scheduleList;
}
