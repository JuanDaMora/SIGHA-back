package judamov.sipoh.controllers;

import judamov.sipoh.dto.ScheduleCreateDTO;
import judamov.sipoh.service.impl.ScheduleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedule")
public class ScheduleController {
    private final ScheduleServiceImpl scheduleService;

    @PutMapping
    public ResponseEntity<Boolean> updateSchedulesBydGroup(
            @RequestBody List<ScheduleCreateDTO> scheduleUpdateDTOS,
            @RequestHeader Long userId
            ){
        return ResponseEntity.ok(scheduleService.updateScheduleGroup(scheduleUpdateDTOS,userId));
    }
}
