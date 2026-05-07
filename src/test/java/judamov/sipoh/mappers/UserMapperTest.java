package judamov.sipoh.mappers;

import judamov.sipoh.dto.UserBasicUpdateDTO;
import judamov.sipoh.dto.UserDTO;
import judamov.sipoh.entity.Role;
import judamov.sipoh.entity.TypeDocument;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;
import judamov.sipoh.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper")
class UserMapperTest {

    @Test
    @DisplayName("userToUserDTO - null retorna null")
    void shouldReturnNullForNullUser() {
        assertThat(UserMapper.userToUserDTO(null, List.of())).isNull();
    }

    @Test
    @DisplayName("userToUserDTO - mapea usuario y roles filtrados")
    void shouldMapUserAndRoles() {
        User user = TestDataFactory.buildTeacherUser();
        Role role = TestDataFactory.buildRoleProfesor();
        UserRol ur = TestDataFactory.buildUserRol(user, role, TestDataFactory.buildProgram());

        UserDTO dto = UserMapper.userToUserDTO(user, List.of(ur));

        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getIdsRoles()).containsExactly(role.getId());
        assertThat(dto.getRolesDescriptions()).containsExactly(role.getName());
    }

    @Test
    @DisplayName("userDTOtoUser - null DTO retorna null")
    void shouldReturnNullForNullDto() {
        assertThat(UserMapper.userDTOtoUser(null, TestDataFactory.buildTypeDocument())).isNull();
    }

    @Test
    @DisplayName("userDTOtoUser - construye entidad desde DTO")
    void shouldBuildUserFromDto() {
        TypeDocument td = TestDataFactory.buildTypeDocument();
        UserDTO dto = UserDTO.builder()
                .id(5L)
                .email("e@test.com")
                .documento("123")
                .firstName("F")
                .lastName("L")
                .isActive(true)
                .build();

        User u = UserMapper.userDTOtoUser(dto, td);

        assertThat(u.getEmail()).isEqualTo("e@test.com");
        assertThat(u.getTypeDocument()).isEqualTo(td);
        assertThat(u.getActive()).isTrue();
    }

    @Test
    @DisplayName("updateUserBasicFields - no hace nada si algún argumento es null")
    void shouldNoOpWhenNullArgs() {
        User u = TestDataFactory.buildTeacherUser();
        UserBasicUpdateDTO dto = UserBasicUpdateDTO.builder().email("x").build();
        UserMapper.updateUserBasicFields(null, dto, TestDataFactory.buildTypeDocument());
        UserMapper.updateUserBasicFields(u, null, TestDataFactory.buildTypeDocument());
        UserMapper.updateUserBasicFields(u, dto, null);
        assertThat(u.getEmail()).isNotEqualTo("x");
    }

    @Test
    @DisplayName("updateUserBasicFields - actualiza campos del usuario")
    void shouldUpdateBasicFields() {
        User u = TestDataFactory.buildTeacherUser();
        TypeDocument td = TestDataFactory.buildTypeDocument();
        UserBasicUpdateDTO dto = UserBasicUpdateDTO.builder()
                .email("nuevo@test.com")
                .documento("999")
                .firstName("FN")
                .lastName("LN")
                .idTipoDocumento(td.getId())
                .build();

        UserMapper.updateUserBasicFields(u, dto, td);

        assertThat(u.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(u.getDocumento()).isEqualTo("999");
        assertThat(u.getFirstName()).isEqualTo("FN");
        assertThat(u.getLastName()).isEqualTo("LN");
        assertThat(u.getTypeDocument()).isEqualTo(td);
    }
}
