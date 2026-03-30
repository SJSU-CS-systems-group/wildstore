package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

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

    protected User() {}

    public User(String email, String firstName, String lastName) {
      this.email = email;
      this.firstName = firstName;
      this.lastName = lastName;
    }
}
