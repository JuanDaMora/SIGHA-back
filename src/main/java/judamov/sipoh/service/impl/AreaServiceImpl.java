package judamov.sipoh.service.impl;

import jakarta.transaction.Transactional;
import judamov.sipoh.dto.AreaDTO;
import judamov.sipoh.dto.AreaSubjectDTO;
import judamov.sipoh.dto.SubjectDTO;
import judamov.sipoh.entity.Area;
import judamov.sipoh.entity.Subject;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IAreaRepository;
import judamov.sipoh.repository.ISubjectRepository;
import judamov.sipoh.service.interfaces.IAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl  implements IAreaService {

    private final IAreaRepository areaRepository;
    private final ISubjectRepository subjectRepository;
    @Override
    public List<AreaSubjectDTO> getAllAreas() {
        // Obtener todas las materias con sus áreas asociadas
        List<Subject> allSubjects = subjectRepository.findAll();

        // Agrupar materias por área
        Map<Area, List<Subject>> groupedByArea = allSubjects.stream()
                .collect(Collectors.groupingBy(Subject::getArea));

        // Mapear a DTOs
        return groupedByArea.entrySet().stream()
                .map(entry -> {
                    Area area = entry.getKey();
                    List<SubjectDTO> subjectDTOs = entry.getValue().stream()
                            .map(subject -> SubjectDTO.builder()
                                    .id(subject.getId())
                                    .name(subject.getName())
                                    .code(subject.getCodigo())
                                    .idLevel(subject.getLevelSubject().getId())
                                    .level(subject.getLevelSubject().getDescription())
                                    .build())
                            .toList();

                    return AreaSubjectDTO.builder()
                            .id(area.getId())
                            .description(area.getDescription())
                            .subjectList(subjectDTOs)
                            .build();
                })
                .toList();
    }


    @Transactional
    @Override
    public Boolean createArea(AreaDTO areaDTO) {
        areaRepository.findOneByDescription(areaDTO.getDescription().toUpperCase())
                .ifPresent(u -> {
                    throw new GenericAppException(HttpStatus.BAD_REQUEST, "El area ya existe");
                });

        Area newArea = Area.builder()
                .description(areaDTO.getDescription().toUpperCase())
                .build();
        try {
            areaRepository.save(newArea);
        } catch (Exception e) {
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado al guardar el Area");
        }

        return true;
    }
    @Transactional
    @Override
    public Boolean updateArea(Long idArea, AreaDTO areaDTO){
        Area area=areaRepository.findOneById(idArea)
                .orElseThrow(
                        ()-> new GenericAppException(HttpStatus.NOT_FOUND,
                                "No se encontro el area con id: "+areaDTO.getId())
                );
        area.setDescription(areaDTO.getDescription());
        try{
            areaRepository.save(area);
        }catch (Exception e){
            throw new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar el area con id: "+ areaDTO.getId());
        }
        return true;
    }

}
