package judamov.sipoh.mappers;

import judamov.sipoh.dto.GroupDTO;
import judamov.sipoh.entity.Group;

import java.util.List;
import java.util.stream.Collectors;

public class GroupMapper {

    public static GroupDTO toDTO(Group group) {
        if (group == null) return null;

        return GroupDTO.builder()
                .id(group.getId())
                .code(group.getCode())
                .idSemestre(group.getSemester() != null ? group.getSemester().getId() : null)
                .idSubject(group.getSubject() != null ? group.getSubject().getId() : null)
                .codeSubject(group.getSubject() != null ? group.getSubject().getCodigo() : null)
                .nameSubject(group.getSubject() != null ? group.getSubject().getName() : null)
                .idDocente(group.getDocente() != null ? group.getDocente().getId() : null)
                .max_students(group.getMax_students() != null ? group.getMax_students() : null)
                .enrolled(group.getEnrolled() != null ? group.getEnrolled() : null)
                .idLevel(group.getSubject() != null && group.getSubject().getLevelSubject() != null
                        ? group.getSubject().getLevelSubject().getId()
                        : null)
                .levelName(group.getSubject() != null && group.getSubject().getLevelSubject() != null
                        ? group.getSubject().getLevelSubject().getDescription()
                        : null)
                .build();
    }

    public static List<GroupDTO> toDTOList(List<Group> groups) {
        return groups.stream()
                .map(GroupMapper::toDTO)
                .collect(Collectors.toList());
    }
}
