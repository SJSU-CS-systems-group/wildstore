package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ZipEntryRecord(
    @JsonProperty("entryName") String entryName, 
    @JsonProperty("entryPath") String entryPath,
    @JsonProperty("type") String type){}
