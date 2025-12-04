package judamov.sipoh.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import judamov.sipoh.dto.GroupCreateDTO;
import judamov.sipoh.dto.GroupDTO;
import judamov.sipoh.dto.GroupUpdateDTO;
import judamov.sipoh.service.impl.GroupServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/group")
public class GroupController {

    private final GroupServiceImpl groupService;

    @GetMapping("/by-semesters")
    public ResponseEntity<List<GroupDTO>> getAllBySemesters(
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        List<GroupDTO> groups = groupService.getAllBySemester(userId, semesterId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/by-levels")
    public ResponseEntity<List<GroupDTO>> getAllByLevels(
            @RequestParam List<Long> idLevels,
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        List<GroupDTO> groups = groupService.getAllByLevels(idLevels, userId, semesterId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/by-subject")
    public ResponseEntity<List<GroupDTO>> getAllBySubject(
            @RequestParam Long subjectId,
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        List<GroupDTO> groups = groupService.getAllBySubject(subjectId, userId, semesterId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/by-docente")
    public ResponseEntity<List<GroupDTO>> getAllByDocente(
            @RequestParam Long docenteId,
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        List<GroupDTO> groups = groupService.getAllByDocente(docenteId, userId, semesterId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/by-filters")
    public ResponseEntity<List<GroupDTO>> getAllByFilters(
            @RequestParam(required = false) List<Long> idLevels,
            @RequestParam(required = false) List<Long> docentesIds,
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(required = true)  Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {

        List<GroupDTO> groups = groupService.getAllByFilters(idLevels, docentesIds, subjectIds, userId, semesterId);
        return ResponseEntity.ok(groups);
    }


    @PutMapping("/{groupId}")
    public ResponseEntity<Boolean> updateGroup(
            @PathVariable Long groupId,
            @RequestHeader Long semesterId,
            @RequestBody GroupUpdateDTO dto,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        return ResponseEntity.ok( groupService.updateGroup(groupId, dto, userId, semesterId));
    }
    @PostMapping
    public ResponseEntity<Boolean> createGroup(
            @RequestBody GroupCreateDTO dto,
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        return ResponseEntity.ok(groupService.createGroup(dto, userId, semesterId));
    }
    @PostMapping("/bulk")
    public ResponseEntity<Boolean> createBulkGroups(
            @RequestBody List<GroupCreateDTO> dtos,
            @RequestHeader Long semesterId,
            @Parameter(hidden = true) @RequestHeader Long userId) {
        return ResponseEntity.ok(groupService.createGroupsBulk(dtos, userId, semesterId));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteGroup(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader Long userId
    ){
        return ResponseEntity.ok(groupService.deleteGroup(id,userId));
    }
    @DeleteMapping("/bySemester/{semesterId}")
    public ResponseEntity<Boolean> deleteAllGroupsBySemesterId(
            @Parameter(hidden = true) @RequestHeader Long userId,
            @PathVariable Long semesterId
    ) {
        return ResponseEntity.ok(groupService.deleteAllGroupsBySemesterId(userId,semesterId));
    }


}
