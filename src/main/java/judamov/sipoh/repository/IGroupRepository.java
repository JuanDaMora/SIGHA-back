package judamov.sipoh.repository;

import judamov.sipoh.entity.Group;
import judamov.sipoh.entity.Semester;
import judamov.sipoh.entity.Subject;
import judamov.sipoh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IGroupRepository extends JpaRepository<Group, Long> {
    List<Group> findBySemester(Semester semester);

    Optional<List<Group>> findBySubjectAndSemester(Subject subject, Semester semester);

    Optional<List<Group>> findByDocenteAndSemester (User docente, Semester semester);

    Optional<Group> findByCode(String code);

    /**
     * Consulta nativa sobre la vista core.v_all_groups_by_semester
     * que devuelve todos los grupos de todos los programas para
     * un semestre dado.
     *
     * El resultado es una lista de arreglos de Object con el siguiente orden:
     * 0: id
     * 1: id_semestre
     * 2: id_subject
     * 3: code_subject
     * 4: name_subject
     * 5: id_docente
     * 6: id_level
     * 7: level_name
     * 8: code
     * 9: max_students
     * 10: enrolled
     * 11: program_code
     * 12: program_name
     * 13: escuela
     */
    @Query(value = """
            SELECT
                id,
                id_semestre,
                id_subject,
                code_subject,
                name_subject,
                id_docente,
                id_level,
                level_name,
                code,
                max_students,
                enrolled,
                program_code,
                program_name,
                escuela
            FROM core.v_all_groups_by_semester
            WHERE id_semestre = :semesterId
            """,
            nativeQuery = true)
    List<Object[]> findAllProgramsBySemester(@Param("semesterId") Long semesterId);
}
