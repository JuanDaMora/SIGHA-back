package judamov.sipoh.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import judamov.sipoh.dto.EmailRequestDTO;
import judamov.sipoh.dto.RecoveryPasswordRequestDTO;
import judamov.sipoh.service.impl.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailServiceImpl emailService;

    @PostMapping("/send-credentials")
    public ResponseEntity<Boolean> sendCredentials(
            @RequestHeader Long userId,
            @RequestBody EmailRequestDTO request) {
        return ResponseEntity.ok(emailService.sendEmail(userId, request));
    }
}
