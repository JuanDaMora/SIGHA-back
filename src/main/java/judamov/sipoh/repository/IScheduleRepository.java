package judamov.sipoh.repository;

import judamov.sipoh.entity.Group;
import judamov.sipoh.entity.Schedule;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.User;
import judamov.sipoh.enums.DayOfWeekEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IScheduleRepository extends JpaRepository<Schedule,Long> {
    Optional<List<Schedule>> findByGroup(Group group);
    void deleteByGroupIn(List<Group> groups);
    // Para crear (no hay grupo previo que excluir)
    boolean existsByGroupDocenteAndGroupSemesterAndDayAndStartTime(
            User docente,
            Semester semester,
            DayOfWeekEnum day,
            LocalTime startTime
    );

    // Para actualizar (excluir el mismo grupo)
    boolean existsByGroupDocenteAndGroupSemesterAndDayAndStartTimeAndGroupIdNot(
            User docente,
            Semester semester,
            DayOfWeekEnum day,
            LocalTime startTime,
            Long groupId
    );
}
