package judamov.sipoh.controllers.publicos;

import judamov.sipoh.dto.AuthResponse;
import judamov.sipoh.dto.LoginRequest;
import judamov.sipoh.dto.RegisterRequest;
import judamov.sipoh.dto.RegisterResponse;
import judamov.sipoh.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/auth")
@RequiredArgsConstructor
public class AuthPublicController {

    private final AuthServiceImpl authServiceImpl;

    @PostMapping(value="login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authServiceImpl.login(request));
    }

//    @PostMapping(value="register")
//    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request){
//        return ResponseEntity.ok(authServiceImpl.register(request));
//    }
}
