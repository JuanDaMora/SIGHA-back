package judamov.sipoh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualAvailabilityDTO {
    private Long id;          // id del registro en la tabla
    private Long userId;      // id del docente
    private Long semesterId;  // id del semestre
    private Boolean isActive; // estado actual
}
