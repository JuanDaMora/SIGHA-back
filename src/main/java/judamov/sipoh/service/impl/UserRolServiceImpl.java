package judamov.sipoh.service.impl;

import judamov.sipoh.dto.UserDTO;
import judamov.sipoh.entity.Program;
import judamov.sipoh.entity.Role;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;
import judamov.sipoh.exceptions.GenericAppException;
import judamov.sipoh.repository.IProgramRepository;
import judamov.sipoh.repository.IUserRepository;
import judamov.sipoh.repository.IUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRolServiceImpl {
    private final IUserRoleRepository userRoleRepository;
    private final IUserRepository userRepository;
    private final IProgramRepository programRepository;
    
    @Value("${spring.jpa.properties.hibernate.default_schema:ing_sistemas}")
    private String currentSchema;

    public List<Role> getRolListFromUser(User user){
        Program currentProgram = programRepository.findByCode(currentSchema)
                .orElseThrow(() -> new GenericAppException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Programa no encontrado para el schema: " + currentSchema));
        
        List<UserRol> userRolList = userRoleRepository.findAllByUserAndProgram(user, currentProgram)
                .orElse(new ArrayList<>());
        
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
