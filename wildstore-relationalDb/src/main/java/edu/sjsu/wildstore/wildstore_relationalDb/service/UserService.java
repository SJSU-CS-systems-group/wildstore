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

    public User findOrCreate(String email) {
      String firstName = "";
      String lastName = "";
      return findOrCreate(email, firstName, lastName);
    }

    @Transactional
    public User findOrCreate(String email, String firstName, String lastName) {
        // Check if the entity already exists
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        } 
        // If not found, create a new instance
        User user = new User(email, firstName, lastName, UserRole.EXTERNAL); // Assuming a constructor or setters
        return userRepository.save(user); // Save the new entity
    }

    @Transactional
    public User findOrCreateInternal(String email, String firstName, String lastName) {
      User user = findOrCreate(email, firstName, lastName);
      user.setUserRole(UserRole.INTERNAL);
      return userRepository.save(user); // Save the new entity
    }

    public User findOrCreateAdmin(String email) {
      String firstName = "";
      String lastName = "";
      return findOrCreateAdmin(email, firstName, lastName);
    }

    @Transactional
    public User findOrCreateAdmin(String email, String firstName, String lastName) {
      User user = findOrCreate(email, firstName, lastName);
      user.setUserRole(UserRole.ADMIN);
      return userRepository.save(user); // Save the new entity
    }

    public boolean isAdmin(Long userId) {
      return userRepository.existsByIdAndUserRole(userId, UserRole.ADMIN);
    }

    public Optional<User> getAdminById(Long adminId) {
      return userRepository.findByIdAndUserRole(adminId, UserRole.ADMIN);
    }

    public Optional<User> getUserById(Long userId) {
      return userRepository.findById(userId);
    }
}
