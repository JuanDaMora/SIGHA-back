package judamov.sipoh.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import judamov.sipoh.dto.SemesterDTO;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.service.impl.SemesterServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterServiceImpl semesterService;
    @GetMapping
    public ResponseEntity<List<Semester>> getSemesters(){
        return ResponseEntity.ok(semesterService.getAllSemesters());
    }

    @PostMapping
    public ResponseEntity<Boolean> createSemester(@RequestBody SemesterDTO semesterDTO){
        return ResponseEntity.ok(semesterService.addSemester(semesterDTO));
    }

    @PutMapping
    public ResponseEntity<SemesterDTO> updateSemester(@RequestBody SemesterDTO semesterDTO){
        return ResponseEntity.ok(semesterService.updateSemester(semesterDTO));
    }

    @PutMapping("/availability")
    public ResponseEntity<Boolean> changeAvailability(
            @RequestParam Boolean newAvailability,
            @RequestParam Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId){
        return ResponseEntity.ok(semesterService.changeAvailability(newAvailability,semesterId,userId));
    }
}
