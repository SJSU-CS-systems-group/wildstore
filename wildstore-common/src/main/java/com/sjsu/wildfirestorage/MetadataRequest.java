package com.sjsu.wildfirestorage;

public class MetadataRequest {
    public String searchQuery;

    public int offset = 0;

    public int limit = 10;

    public String[] includeFields;

    public String[] excludeFields;
}
