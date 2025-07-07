package judamov.sipoh.repository;

import judamov.sipoh.entity.Group;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.Subject;
import judamov.sipoh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IGroupRepository extends JpaRepository<Group, Long> {
    List<Group> findBySemester(Semester semester);

    Optional<List<Group>> findBySubjectAndSemester(Subject subject, Semester semester);

    Optional<List<Group>> findByDocenteAndSemester (User docente, Semester semester);

    Optional<Group> findByCode(String code);
}
