package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BulkZipInfoRecord(
    @JsonProperty("zipInfoRecords") List<ZipInfoRecord> zipInfoRecords){}
