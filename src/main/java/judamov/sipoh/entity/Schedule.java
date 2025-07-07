package judamov.sipoh.entity;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import judamov.sipoh.enums.DayOfWeekEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
@Entity
@Data
@Getter
@Transactional
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne()
    @JoinColumn(name = "id_group", nullable = false)
    private Group group;
    @Column(name="start_time")
    LocalTime startTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeekEnum day;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name= "updated_at")
    private LocalDateTime updatedAt;
}
