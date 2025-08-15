package judamov.sipoh.controllers;
import judamov.sipoh.dto.*;
import judamov.sipoh.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para endpoints privados relacionados con autenticación y gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthPrivateController {

    private final AuthServiceImpl authService;

    /**
     * Obtiene los datos del usuario autenticado usando su ID proporcionado en el header.
     *
     * @param userId ID del usuario (enviado en el header "user-id")
     * @return datos del usuario en formato {@link UserDTO}
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserDTO> getMyData(@RequestHeader String userId) {
        return ResponseEntity.ok(authService.getOwnUserData(Long.parseLong(userId)));
    }

    /**
     * Retorna todos los usuarios del sistema con sus datos básicos, áreas asociadas y último acceso.
     *
     * @return lista de usuarios {@link UserDTO}
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(){
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * Obtiene la información detallada de un usuario específico, solo si tiene un rol autorizado.
     *
     * @param id ID del usuario a consultar
     * @return datos del usuario en formato {@link UserDTO}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id, @RequestHeader Long userId){
        return ResponseEntity.ok(authService.getUserById(id,userId));
    }

    /**
     * Cambia la contraseña de un usuario autenticado, validando la contraseña actual.
     *
     * @param resetpassword objeto con documento, contraseña actual y nueva contraseña
     * @return respuesta con el nuevo token generado {@link ChangePasswordResponse}
     */
    @PostMapping("/changePassword")
    public ResponseEntity<ChangePasswordResponse> changePassword(@RequestBody ChangePasswordDTO resetpassword) {
        return ResponseEntity.ok(authService.changePassword(resetpassword));
    }

    /**
     * Actualiza los datos de un usuario, siempre y cuando tenga un rol autorizado para ello.
     *
     * @param id ID del usuario a actualizar
     * @param userDTO datos nuevos del usuario
     * @return true si la operación fue exitosa
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Boolean> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO, @RequestHeader Long userId){
        return ResponseEntity.ok(authService.updateUser(userId, id, userDTO));
    }

    @PutMapping("/users/me")
    public ResponseEntity<Boolean> updateUserMe(@RequestHeader Long userId,@RequestBody UserBasicUpdateDTO userDTO){
        return ResponseEntity.ok(authService.updateUserMe(userId, userDTO));
    }

    /**
     * Endpoint para registrar múltiples usuarios desde un archivo o fuente externa.
     *
     * @param userBulkDTOList lista de usuarios con datos básicos
     * @return true si se registraron correctamente
     */
    @PostMapping("/users/bulk-register")
    public ResponseEntity<Boolean> bulkRegisterUsers(
            @RequestBody List<BulkUserDTO> userBulkDTOList,
            @RequestHeader Long userId,
            @RequestParam Boolean isFakeEmail
    ) {
        return ResponseEntity.ok(authService.registerBulkUsers(userId,userBulkDTOList, isFakeEmail));
    }

    @PostMapping(value="register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request,
            @RequestHeader Long userId
    ){
        return ResponseEntity.ok(authService.register(request,userId));
    }
}
