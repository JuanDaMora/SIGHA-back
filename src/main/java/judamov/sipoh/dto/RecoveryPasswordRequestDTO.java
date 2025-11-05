package judamov.sipoh.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryPasswordRequestDTO {
    private String documento;
    private boolean isFake;
}
