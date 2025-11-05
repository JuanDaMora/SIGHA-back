package judamov.sipoh.controllers.publicos;

import judamov.sipoh.dto.*;
import judamov.sipoh.service.impl.AuthServiceImpl;
import judamov.sipoh.service.impl.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/auth")
@RequiredArgsConstructor
public class AuthPublicController {

    private final AuthServiceImpl authServiceImpl;
    private final EmailServiceImpl emailService;

    @PostMapping(value="login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authServiceImpl.login(request));
    }

//    @PostMapping(value="register")
//    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request){
//        return ResponseEntity.ok(authServiceImpl.register(request));
//    }
// ============================================================
// ENVÍO DE RECUPERACIÓN DE CONTRASEÑA
// ============================================================
    @PostMapping("/send-recovery")
    public ResponseEntity<Boolean> sendRecovery(
            @RequestBody RecoveryPasswordRequestDTO request) {
        return ResponseEntity.ok(
                emailService.sendRecoveryPassword(request.getDocumento(), request.isFake())
        );
    }
}
