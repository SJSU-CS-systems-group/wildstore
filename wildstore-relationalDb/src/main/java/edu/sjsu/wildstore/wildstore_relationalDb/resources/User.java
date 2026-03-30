package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String firstName; 

    private String lastName; 

    @Column(unique = true, nullable = false)
    @Convert(converter = UserEmailConverter.class)
    private String email; 

    protected User() {}

    public User(String email, String firstName, String lastName) {
      this.email = email;
      this.firstName = firstName;
      this.lastName = lastName;
    }
}
