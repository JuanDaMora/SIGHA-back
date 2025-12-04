package judamov.sipoh.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import judamov.sipoh.dto.AvailabilityDTO;
import judamov.sipoh.dto.GlobalAvabilityDTO;
import judamov.sipoh.dto.IndividualAvailabilityDTO;
import judamov.sipoh.service.impl.AvailabilityServiceImpl;
import judamov.sipoh.service.impl.IndividualAvailabilityImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityServiceImpl availabilityService;
    private final IndividualAvailabilityImpl individualAvailabilityService;

    // ----------------- GLOBAL AVAILABILITY -----------------

    @GetMapping("/global")
    public ResponseEntity<List<GlobalAvabilityDTO>> getGlobalAvailability(
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId
    ) {
        return ResponseEntity.ok(availabilityService.getListGlobalAvailability(semesterId, userId));
    }

    @GetMapping("/global/by-subjects")
    public ResponseEntity<List<GlobalAvabilityDTO>> getGlobalAvailabilityBySubjects(
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId
    ) {
        return ResponseEntity.ok(
                availabilityService.getGlobalAvailabilityBySubjects(semesterId, userId, subjectIds)
        );
    }

    @GetMapping("/global/{docentId}")
    public ResponseEntity<GlobalAvabilityDTO> getGlobalAvailability(
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId,
            @PathVariable String docentId
    ) {
        return ResponseEntity.ok(
                availabilityService.getAvailabilityDTO(userId, semesterId, Long.parseLong(docentId))
        );
    }

    // ----------------- DOCENTE AVAILABILITY -----------------

    @GetMapping("/docente")
    public ResponseEntity<AvailabilityDTO> getAvailability(
            @Parameter(hidden = true) @RequestHeader Long userId,
            @RequestHeader Long semesterId
    ) {
        AvailabilityDTO dto = availabilityService.getAvailabilityByIdDocent(userId, semesterId);
        return ResponseEntity.ok(dto);
    }

    // ----------------- INDIVIDUAL AVAILABILITY -----------------

    @PostMapping("/individual")
    public ResponseEntity<IndividualAvailabilityDTO> upsertIndividualAvailability(
            @Parameter(hidden = true) @RequestHeader Long userId,
            @RequestHeader Long semesterId,
            @RequestParam Long docenteId,
            @RequestParam boolean isActive
    ) {
        return ResponseEntity.ok(
                individualAvailabilityService.upsertIndividualAvailability(semesterId, userId, isActive, docenteId)
        );
    }

    @GetMapping("/individual")
    public ResponseEntity<IndividualAvailabilityDTO> getIndividualAvailability(
            @Parameter(hidden = true) @RequestHeader Long userId,
            @RequestHeader Long semesterId,
            @RequestParam Long docenteId
    ) {
        return ResponseEntity.ok(
                individualAvailabilityService.getStatusIndividualAvailability(semesterId, userId, docenteId)
        );
    }

}
