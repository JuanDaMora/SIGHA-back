package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.GroupCreateDTO;
import judamov.sipoh.dto.GroupDTO;
import judamov.sipoh.dto.GroupUpdateDTO;
import judamov.sipoh.entity.Group;

import java.util.List;

public interface IGroupService {
    List<GroupDTO> getAllBySemester(Long adminId, Long semesterId);

    List<GroupDTO> getAllByLevels(List<Long> idLevel, Long adminId, Long semesterId);

    List<GroupDTO> getAllBySubject(Long subjectId, Long adminId, Long semesterId);
    List<GroupDTO> getAllByDocente(Long docenteId, Long adminId, Long semesterId);

    Boolean createGroup(GroupCreateDTO dto, Long adminId,Long semesterId);
    Boolean updateGroup(Long groupId, GroupUpdateDTO dto, Long adminId, Long semesterId);

}
