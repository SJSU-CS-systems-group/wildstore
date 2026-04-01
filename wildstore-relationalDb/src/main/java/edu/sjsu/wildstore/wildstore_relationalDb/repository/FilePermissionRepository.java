package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {
  List<FilePermission> findByFileNodeId(Long fileNodeId);
  boolean existsByUserIdAndFileNodeId(Long userId, Long fileNodeId);
  Optional<FilePermission> findByUserIdAndFileNodeId(Long userId, Long fileNodeId);
}
