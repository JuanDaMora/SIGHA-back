package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.LevelSubjectDTO;
import judamov.sipoh.entity.LevelSubject;

import java.util.List;

public interface ILevelSubjectService {
    List<LevelSubjectDTO> getAll(Long adminId);
}
