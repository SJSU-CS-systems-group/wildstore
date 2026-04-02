package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByIdAndUserRole(Long userId, UserRole userRole);

  Optional<User> findByIdAndUserRole(Long userId, UserRole userRole);

  Optional<User> findById(Long userId);
}
