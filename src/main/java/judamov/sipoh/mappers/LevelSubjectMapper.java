package judamov.sipoh.mappers;

import judamov.sipoh.dto.LevelSubjectDTO;
import judamov.sipoh.entity.LevelSubject;

import java.util.List;
import java.util.stream.Collectors;

public class LevelSubjectMapper {
    public static LevelSubjectDTO toDTO(LevelSubject levelSubject){
        if(levelSubject==null) return null;

        return LevelSubjectDTO.builder()
                .id(levelSubject.getId())
                .name(levelSubject.getDescription())
                .build();
    }
    public static List<LevelSubjectDTO> toDTOList(List<LevelSubject> levelSubjectDTOList){
        return levelSubjectDTOList.stream()
                .map(LevelSubjectMapper::toDTO)
                .collect(Collectors.toList());
    }
}
