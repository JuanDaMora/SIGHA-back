package judamov.sipoh.service;

import judamov.sipoh.dto.RoleDTO;
import judamov.sipoh.entity.Role;
import judamov.sipoh.repository.IRoleRepository;
import judamov.sipoh.service.impl.RoleServiceImpl;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl - Tests unitarios")
class RoleServiceImplTest {

    @Mock
    private IRoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("getAllRoles - mapea cada rol a RoleDTO con id y nombre")
    void shouldMapAllRolesToDto() {
        Role r1 = TestDataFactory.buildRoleAdmin();
        Role r2 = TestDataFactory.buildRoleProfesor();
        when(roleRepository.findAll()).thenReturn(List.of(r1, r2));

        List<RoleDTO> result = roleService.getAllRoles();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(r1.getId());
        assertThat(result.get(0).name()).isEqualTo(r1.getName());
        assertThat(result.get(1).name()).isEqualTo(r2.getName());
    }

    @Test
    @DisplayName("getAllRoles - retorna lista vacía cuando no hay roles en repositorio")
    void shouldReturnEmptyListWhenNoRoles() {
        when(roleRepository.findAll()).thenReturn(List.of());

        List<RoleDTO> result = roleService.getAllRoles();

        assertThat(result).isEmpty();
    }
}
