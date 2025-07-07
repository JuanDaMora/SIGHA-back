package judamov.sipoh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectCreateDTO {
    String code;
    Long idArea;
    Long idLevel;
    String name;
    Integer maxStudents;
}
