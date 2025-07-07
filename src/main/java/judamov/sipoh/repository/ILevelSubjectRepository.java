package judamov.sipoh.repository;

import judamov.sipoh.entity.LevelSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ILevelSubjectRepository extends JpaRepository<LevelSubject,Long> {
}
