package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName; 

    private String lastName; 

    @Column(unique = true, nullable = false)
    @Convert(converter = UserEmailConverter.class)
    private String email; 

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<FilePermission> userPermissions;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.REMOVE)
    private List<FilePermission> grantedPermissions;

    protected User() {}

    public User(String email, String firstName, String lastName, UserRole userRole) {
      this.email = email;
      this.firstName = firstName;
      this.lastName = lastName;
      this.userRole = userRole;
    }

    public Long getId() {
      return this.id;
    }

    public void setUserRole(UserRole userRole) {
      this.userRole = userRole;
    }

}
