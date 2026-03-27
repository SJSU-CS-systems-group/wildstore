package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class FileNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String file_name; 

    private long size; 

    @Enumerated(EnumType.STRING)
    private FileType file_type; 

    @Convert(converter = FileDigestConverter.class)
    private String digest;
}
