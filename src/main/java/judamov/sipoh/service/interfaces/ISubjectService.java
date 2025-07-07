package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.SubjectCreateDTO;

public interface ISubjectService {
    Boolean createSubject(SubjectCreateDTO subjectCreateDTO, Long adminId);
}
