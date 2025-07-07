package judamov.sipoh.controllers;

import judamov.sipoh.dto.SubjectCreateDTO;
import judamov.sipoh.service.impl.SubjectServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subject")
public class SubjectController {

    private final SubjectServiceImpl subjectService;
    @PostMapping
    public ResponseEntity<Boolean> createSubject(
            @RequestBody SubjectCreateDTO subjectCreateDTO,
            @RequestHeader Long userId
            ){
        return ResponseEntity.ok(subjectService.createSubject(subjectCreateDTO,userId));
    }
}
