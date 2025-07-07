package judamov.sipoh.controllers;

import judamov.sipoh.dto.LevelSubjectDTO;
import judamov.sipoh.service.impl.LevelSubjectServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/level-subject")
@RequiredArgsConstructor
public class LevelSubjectController {
    private final LevelSubjectServiceImpl levelSubjectService;

    @GetMapping
    public ResponseEntity<List<LevelSubjectDTO>> getAll(
            @RequestHeader Long userId){
        return ResponseEntity.ok(levelSubjectService.getAll(userId));
    }
}
