package judamov.sipoh.mappers;

import judamov.sipoh.dto.UserBasicUpdateDTO;
import judamov.sipoh.dto.UserDTO;
import judamov.sipoh.entity.TypeDocument;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    /**
     * Convierte User a UserDTO filtrando roles por programa específico.
     * IMPORTANTE: Este es el único método válido para convertir User a UserDTO.
     * Siempre debe recibir la lista de roles filtrados por programa.
     */
    public static UserDTO userToUserDTO(User user, List<UserRol> filteredUserRoles) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .idTipoDocumento(user.getTypeDocument().getId())
                .documento(user.getDocumento())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getActive())
                .idsRoles(filteredUserRoles.stream()
                        .map(userRol -> userRol.getRole().getId())
                        .toList())
                .rolesDescriptions(filteredUserRoles.stream()
                        .map(userRol -> userRol.getRole().getName())
                        .collect(Collectors.toList()))
                .createAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static User userDTOtoUser(UserDTO dto, TypeDocument typeDocument) {
        if (dto == null) return null;

        return User.builder()
                .id(dto.getId())
                .email(dto.getEmail())
                .typeDocument(typeDocument)
                .documento(dto.getDocumento())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .active(dto.getIsActive())
                .build();
    }

    public static void updateUserBasicFields(User user, UserBasicUpdateDTO dto, TypeDocument typeDocument) {
        if (user == null || dto == null || typeDocument == null) return;

        user.setEmail(dto.getEmail());
        user.setTypeDocument(typeDocument);
        user.setDocumento(dto.getDocumento());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
    }
}
