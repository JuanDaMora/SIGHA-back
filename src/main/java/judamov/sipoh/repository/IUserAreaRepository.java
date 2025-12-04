package judamov.sipoh.repository;

import judamov.sipoh.entity.UserArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserAreaRepository extends JpaRepository<UserArea,Long> {
    List<UserArea> findByUserId(Long userId);

    @Query("SELECT ua.user.id FROM UserArea ua WHERE ua.area.id IN :areaIds")
    List<Long> findDocentsByAreaIds(@Param("areaIds") List<Long> areaIds);

}
