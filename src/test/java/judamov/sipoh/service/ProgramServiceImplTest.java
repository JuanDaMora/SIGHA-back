package judamov.sipoh.service;

import judamov.sipoh.dto.ProgramDTO;
import judamov.sipoh.entity.Program;
import judamov.sipoh.repository.IProgramRepository;
import judamov.sipoh.service.impl.ProgramServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgramServiceImpl - Tests unitarios")
class ProgramServiceImplTest {

    @Mock
    private IProgramRepository programRepository;

    @InjectMocks
    private ProgramServiceImpl programService;

    @Test
    @DisplayName("getAllPrograms - mapea programas a ProgramDTO")
    void shouldMapProgramsToDto() {
        Program p = TestDataFactory.buildProgram();
        when(programRepository.findAll()).thenReturn(List.of(p));

        List<ProgramDTO> result = programService.getAllPrograms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(p.getId());
        assertThat(result.get(0).name()).isEqualTo(p.getName());
        assertThat(result.get(0).code()).isEqualTo(p.getCode());
    }

    @Test
    @DisplayName("getAllPrograms - retorna lista vacía cuando no hay programas")
    void shouldReturnEmptyListWhenNoPrograms() {
        when(programRepository.findAll()).thenReturn(List.of());

        List<ProgramDTO> result = programService.getAllPrograms();

        assertThat(result).isEmpty();
    }
}
