package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class FilePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@OneToOne(cascade = CascadeType.ALL)
    //@JoinColumn(name="user_id", referencedColumnName="id")
    //private User admin; 
    //
    //@OneToOne(cascade = CascadeType.ALL)
    //@JoinColumn(name="user_id", referencedColumnName="id")
    //private User user;
    //
    //@OneToOne(cascade = CascadeType.ALL)
    //@JoinColumn(name="file_node_id", referencedColumnName="id")
    //private FileNode fileNode;
}
