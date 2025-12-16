package judamov.sipoh.controllers;

import judamov.sipoh.dto.ProgramDTO;
import judamov.sipoh.service.impl.ProgramServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramServiceImpl programService;

    @GetMapping
    public ResponseEntity<List<ProgramDTO>> getAllPrograms() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }
}


