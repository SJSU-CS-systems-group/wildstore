package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileNodeRecord(
    @JsonProperty("file_id") Long id, 
    @JsonProperty("name") String name, 
    @JsonProperty("size") Long size,
    @JsonProperty("digest") String digest,
    @JsonProperty("type") String type){}
