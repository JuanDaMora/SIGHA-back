package judamov.sipoh.service.impl;

import judamov.sipoh.dto.ProgramDTO;
import judamov.sipoh.repository.IProgramRepository;
import judamov.sipoh.service.interfaces.IProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements IProgramService {

    private final IProgramRepository programRepository;

    @Override
    public List<ProgramDTO> getAllPrograms() {
        return programRepository.findAll()
                .stream()
                .map(program -> new ProgramDTO(
                        program.getId(),
                        program.getName(),
                        program.getCode()
                ))
                .toList();
    }
}


