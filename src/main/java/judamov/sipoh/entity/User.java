package judamov.sipoh.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Transactional
@Entity
@Table(name="user", uniqueConstraints = {@UniqueConstraint(columnNames= {"documento"})})
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true)
    String email;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type_document", nullable = false)
    private TypeDocument typeDocument;
    @Column(nullable = false)
    String documento;
    @Column(nullable = false)
    String password;
    @Column(name = "first_name", nullable = false)
    String firstName;
    @Column(name= "last_name", nullable = false)
    String lastName;
    @Column(nullable = false)
    Boolean active;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRol> userRoles;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserArea> userAreas = new ArrayList<>();

    @Column(name= "token_hash")
    private String tokenHash;
    @CreationTimestamp
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name= "update_date")
    private LocalDateTime updatedAt;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userRoles.stream()
                .map(userRol -> new SimpleGrantedAuthority(userRol.getRole().getName()))
                .toList();
    }

    @Override
    public String getUsername() {
        return documento;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return true;
    }
}
