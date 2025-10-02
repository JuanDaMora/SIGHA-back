package judamov.sipoh.repository;

import judamov.sipoh.entity.Availability;
import judamov.sipoh.entity.IndividualAvailability;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IIndividualAvailabilityRepository extends JpaRepository<IndividualAvailability, Long> {
    List<IndividualAvailability> findBySemesterAndUser(Semester semester, User user);
}
