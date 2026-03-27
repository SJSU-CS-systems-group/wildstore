package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FileNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String file_name; 

    private long size; 

    @Enumerated(EnumType.STRING)
    private FileType file_type; 

    @ManyToOne
    @JoinColumn(name="file_node_id", referencedColumnName="id")
    private FileNode parent;

    @Convert(converter = FileDigestConverter.class)
    private String digest;
}
