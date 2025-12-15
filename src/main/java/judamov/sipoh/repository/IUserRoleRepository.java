package judamov.sipoh.repository;

import judamov.sipoh.entity.Program;
import judamov.sipoh.entity.User;
import judamov.sipoh.entity.UserRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserRoleRepository extends JpaRepository<UserRol, Long> {

    boolean findByUser(User user);
    Optional<List<UserRol>> findAllByUser(User user);
    
    @Query("SELECT ur FROM UserRol ur JOIN FETCH ur.role r JOIN FETCH ur.program p WHERE ur.user.id = :userId AND p.id = :programId")
    Optional<List<UserRol>> findAllByUserAndProgram(@Param("userId") Long userId, @Param("programId") Long programId);
    
    default Optional<List<UserRol>> findAllByUserAndProgram(User user, Program program) {
        return findAllByUserAndProgram(user.getId(), program.getId());
    }
}
