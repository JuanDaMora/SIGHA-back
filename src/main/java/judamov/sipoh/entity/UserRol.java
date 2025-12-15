package judamov.sipoh.entity;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_rol_program", schema = "core")
@Transactional
@NoArgsConstructor
@AllArgsConstructor
public class UserRol  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="id_user", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name="id_role", nullable = false)
    private Role role;
    
    @ManyToOne
    @JoinColumn(name="id_program", nullable = false)
    private Program program;
    
    @CreationTimestamp
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name= "update_date")
    private LocalDateTime updatedAt;
}
