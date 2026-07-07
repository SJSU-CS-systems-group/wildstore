package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FilePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="adminId", referencedColumnName="id")
    private User admin; 

    @ManyToOne
    @JoinColumn(name="userId", referencedColumnName="id")
    private User user;

    @ManyToOne
    @JoinColumn(name="fileNodeId", referencedColumnName="id")
    private FileNode fileNode;

    public FilePermission(User admin, User user, FileNode fileNode) {
      this.admin = admin;
      this.user = user;
      this.fileNode = fileNode;
    }

    public FilePermission() {}
}
