package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreate(String email, String firstName, String lastName) {
        // Check if the entity already exists
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            // If not found, create a new instance
            User user = new User(email, firstName, lastName); // Assuming a constructor or setters
            return userRepository.save(user); // Save the new entity
        }
    }

    public boolean isAdmin(Long userId) {
      return userRepository.existsByIdAndUserRole(userId, UserRole.ADMIN);
    }
}
