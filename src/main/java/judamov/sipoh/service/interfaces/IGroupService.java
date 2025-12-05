package judamov.sipoh.service.interfaces;

import judamov.sipoh.dto.GroupCreateDTO;
import judamov.sipoh.dto.GroupDTO;
import judamov.sipoh.dto.GroupUpdateDTO;
import judamov.sipoh.entity.Group;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.User;
import judamov.sipoh.enums.DayOfWeekEnum;

import java.time.LocalTime;
import java.util.List;

public interface IGroupService {
    List<GroupDTO> getAllBySemester(Long adminId, Long semesterId);

    List<GroupDTO> getAllByLevels(List<Long> idLevel, Long adminId, Long semesterId);

    List<GroupDTO> getAllBySubject(Long subjectId, Long adminId, Long semesterId);
    List<GroupDTO> getAllByDocente(Long docenteId, Long adminId, Long semesterId);
    Boolean updateDocente(Long groupId,Long idDocente, Long adminId);

    Boolean createGroup(GroupCreateDTO dto, Long adminId,Long semesterId);
    Boolean updateGroup(Long groupId, GroupUpdateDTO dto, Long adminId, Long semesterId);
    Boolean createGroupsBulk(List<GroupCreateDTO> dtos, Long adminId, Long semesterId) ;
    Boolean deleteAllGroupsBySemesterId (Long userId, Long semesterId);
    List<GroupDTO> getAllByFilters(List<Long> idLevels, List<Long> docentesIds, List<Long> subjectIds, Long adminId,Long semesterId);
}
