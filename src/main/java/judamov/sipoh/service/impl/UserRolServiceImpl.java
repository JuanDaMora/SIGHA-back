package judamov.sipoh.service.impl;

import judamov.sipoh.dto.UserDTO;
import judamov.sipoh.entity.Role;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.repository.IUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRolServiceImpl {
    private final IUserRoleRepository userRoleRepository;
    private final IUserRepository userRepository;

    public List<Role> getRolListFromUser(User user){
        List<UserRol> userRolList = userRoleRepository.findAllByUser(user)
                .orElseThrow(()-> new GenericAppException(HttpStatus.BAD_REQUEST,
                        "No se encontro ningun userRol para el usuario con id: "+ user.getId()));
        List<Role> roleList = new ArrayList<>();
        for(UserRol userRole : userRolList) {
            roleList.add(userRole.getRole());
        }
        return roleList;
    }

    /**
     * Valida si un usuario tiene permisos de administración (Director o Coordinador).
     *
     * @param user usuario a validar
     * @return true si tiene uno de los roles permitidos
     */
    public Boolean hasAdminPrivileges(User user) {
        return getRolListFromUser(user).stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("DIRECTOR DE ESCUELA") ||
                        role.getName().equalsIgnoreCase("COORDINADOR ACADEMICO"));
    }

    public Boolean hasTeacherPrivileges(User user) {
        return getRolListFromUser(user).stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("PROFESOR"));
    }
}
