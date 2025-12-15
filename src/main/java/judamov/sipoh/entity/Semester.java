package judamov.sipoh.entity;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import judamov.sipoh.dto.SemesterDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@Table(name = "semester", schema = "core")
@Transactional
@NoArgsConstructor
@AllArgsConstructor
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String description;
    @Column(unique = true, nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(nullable = false)
    private Boolean availability;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name= "updated_at")
    private LocalDateTime updatedAt;

    public Semester(SemesterDTO semesterDTO) {
        this.description = semesterDTO.getDescription();
        this.startDate = semesterDTO.getStartDate();
        this.endDate = semesterDTO.getEndDate();
        this.availability= semesterDTO.getAvailability();
    }

    public void updateFromDto(SemesterDTO dto) {
        this.description = dto.getDescription();
        this.startDate = dto.getStartDate();
        this.endDate = dto.getEndDate();
    }
}
