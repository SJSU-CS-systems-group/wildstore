package edu.sjsu.wildstore.wildstore_relationalDb.resources;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileNode(
    @JsonProperty("file_id") int id, 
    @JsonProperty("name") String name, 
    @JsonProperty("size") long size,
    @JsonProperty("type") int type,
    @JsonProperty("digest") String digest){}
