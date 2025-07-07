package judamov.sipoh.repository;

import judamov.sipoh.entity.Group;
import judamov.sipoh.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IScheduleRepository extends JpaRepository<Schedule,Long> {
    Optional<List<Schedule>> findByGroup(Group group);
}
