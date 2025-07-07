package judamov.sipoh.entity;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Getter
@Builder
@Transactional
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="id_area",nullable = false)
    private Area area;
    @ManyToOne
    @JoinColumn(name="id_level_subject",nullable = false)
    private LevelSubject levelSubject;
    @Column(name = "codigo")
    private String codigo;
    @Column(name = "name")
    private String name;
    @Column(name= "max_students")
    private Integer max_students;
    @CreationTimestamp
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name= "update_date")
    private LocalDateTime updatedAt;
}
