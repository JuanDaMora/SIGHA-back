package judamov.sipoh.service;

import judamov.sipoh.dto.AreaDTO;
import judamov.sipoh.dto.AreaSubjectDTO;
import judamov.sipoh.entity.Area;
import judamov.sipoh.entity.LevelSubject;
import judamov.sipoh.entity.Subject;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IAreaRepository;
import judamov.sipoh.repository.ISubjectRepository;
import judamov.sipoh.service.impl.AreaServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AreaServiceImpl - Tests unitarios")
class AreaServiceImplTest {

    @Mock private IAreaRepository areaRepository;
    @Mock private ISubjectRepository subjectRepository;

    @InjectMocks
    private AreaServiceImpl areaService;

    private Area area;
    private LevelSubject levelSubject;
    private Subject subject;

    @BeforeEach
    void setUp() {
        area = TestDataFactory.buildArea();
        levelSubject = TestDataFactory.buildLevelSubject();
        subject = TestDataFactory.buildSubject(area, levelSubject);
    }

    @Test
    @DisplayName("getAllAreas - retorna lista de áreas con sus asignaturas agrupadas")
    void shouldReturnAllAreasWithSubjects() {
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(subjectRepository.findAll()).thenReturn(List.of(subject));

        List<AreaSubjectDTO> result = areaService.getAllAreas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(area.getId());
        assertThat(result.get(0).getSubjectList()).hasSize(1);
        assertThat(result.get(0).getSubjectList().get(0).getCode()).isEqualTo("MAT001");
    }

    @Test
    @DisplayName("getAllAreas - retorna área con lista de asignaturas vacía si no tiene materias")
    void shouldReturnAreaWithEmptySubjectsWhenNoSubjectsLinked() {
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(subjectRepository.findAll()).thenReturn(List.of());

        List<AreaSubjectDTO> result = areaService.getAllAreas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubjectList()).isEmpty();
    }

    @Test
    @DisplayName("createArea - crea un área nueva exitosamente")
    void shouldCreateAreaSuccessfully() {
        AreaDTO dto = new AreaDTO();
        dto.setDescription("Física");

        when(areaRepository.findOneByDescription("FÍSICA")).thenReturn(Optional.empty());

        Boolean result = areaService.createArea(dto);

        assertThat(result).isTrue();
        verify(areaRepository).save(any(Area.class));
    }

    @Test
    @DisplayName("createArea - lanza BAD_REQUEST cuando el área ya existe")
    void shouldThrowWhenAreaAlreadyExists() {
        AreaDTO dto = new AreaDTO();
        dto.setDescription("Matemáticas");

        when(areaRepository.findOneByDescription("MATEMÁTICAS")).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> areaService.createArea(dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Ya existe un área con la descripción indicada");
    }

    @Test
    @DisplayName("updateArea - actualiza la descripción del área correctamente")
    void shouldUpdateAreaSuccessfully() {
        AreaDTO dto = new AreaDTO();
        dto.setDescription("Física Avanzada");

        when(areaRepository.findOneById(area.getId())).thenReturn(Optional.of(area));

        Boolean result = areaService.updateArea(area.getId(), dto);

        assertThat(result).isTrue();
        assertThat(area.getDescription()).isEqualTo("Física Avanzada");
        verify(areaRepository).save(area);
    }

    @Test
    @DisplayName("updateArea - lanza NOT_FOUND cuando el área no existe")
    void shouldThrowWhenAreaNotFoundForUpdate() {
        AreaDTO dto = new AreaDTO();
        dto.setDescription("Física");

        when(areaRepository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaService.updateArea(99L, dto))
                .isInstanceOf(GenericAppException.class)
                .hasMessageContaining("Área no encontrada");
    }
}
