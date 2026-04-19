package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.WildfireAttribute;
import edu.sjsu.wildstore.WildfireVariable;
import edu.sjsu.wildstore.meta.MongoCollections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MetadataControllerTest extends BaseControllerTest {

    @BeforeEach
    void setup() {
        mongoTemplate.getDb().getCollection(MongoCollections.METADATA).drop();

        Metadata m = new Metadata();
        m.digestString = "testDigest001";
        m.fileName = new HashSet<>(Set.of("/data/test.nc"));
        m.filePath = new HashSet<>(Set.of("/data/"));
        m.fileType = new HashSet<>(Set.of("nc"));
        m.lastModified = System.currentTimeMillis();
        m.domain = 1;
        m.fileSize = 1024;

        WildfireAttribute attr = new WildfireAttribute();
        attr.attributeName = "Conventions";
        attr.type = "String";
        attr.value = "CF-1.6";
        m.globalAttributes = new ArrayList<>(List.of(attr));

        WildfireVariable var = new WildfireVariable();
        var.variableName = "temperature";
        m.variables = new ArrayList<>(List.of(var));

        mongoTemplate.save(m, MongoCollections.METADATA);
    }

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().getCollection(MongoCollections.METADATA).drop();
    }

    @Test
    void testSearch() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(post("/api/metadata/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"\",\"limit\":10,\"offset\":0}"))
                .andExpect(status().isUnauthorized());

        // Search all, expect: one result
        mockMvc.perform(post("/api/metadata/search")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"\",\"limit\":10,\"offset\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Search with specific query, expect: one result
        mockMvc.perform(post("/api/metadata/search")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"digestString = 'testDigest001'\",\"limit\":10,\"offset\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Count, expect: 1
        mockMvc.perform(post("/api/metadata/search/count")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"\",\"limit\":10,\"offset\":0}"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // Search with includeFields, expect: 200 with 1 result
        mockMvc.perform(post("/api/metadata/search")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"\",\"limit\":10,\"offset\":0,\"includeFields\":[\"digestString\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Search with excludeFields, expect: 200 with 1 result
        mockMvc.perform(post("/api/metadata/search")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"\",\"limit\":10,\"offset\":0,\"excludeFields\":[\"variables\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // searchCount with excludeFields, expect: 1
        mockMvc.perform(post("/api/metadata/search/count")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchQuery\":\"\",\"limit\":10,\"offset\":0,\"excludeFields\":[\"variables\"]}"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void testGetMetadataByDigest() throws Exception {
        // GUEST can access by digest
        mockMvc.perform(get("/api/metadata/testDigest001").with(user("g").roles("GUEST")))
                .andExpect(status().isOk());

        // Unknown digest, expect: null body (empty response)
        mockMvc.perform(get("/api/metadata/unknownDigest").with(user("g").roles("GUEST")))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testGetFileMetadata() throws Exception {
        // Matching filename, expect: 1 result
        mockMvc.perform(get("/api/metadata").param("filename", "test.nc").with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Non-matching filename, expect: empty
        mockMvc.perform(get("/api/metadata").param("filename", "nonexistent.nc").with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUpsertMetadata() throws Exception {
        // Insert a new document, expect: true (created)
        String newDoc = "{\"digestString\":\"newDigest002\","
                + "\"fileName\":[\"/data/new.nc\"],"
                + "\"filePath\":[\"/data/\"],"
                + "\"fileType\":[\"nc\"],"
                + "\"lastModified\":1000000,"
                + "\"domain\":0,\"fileSize\":512,"
                + "\"globalAttributes\":[],\"variables\":[]}";
        mockMvc.perform(post("/api/metadata")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newDoc))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Same filename + same digest, expect: false (already exists, no update needed)
        String dupDoc = "{\"digestString\":\"testDigest001\","
                + "\"fileName\":[\"/data/test.nc\"],"
                + "\"filePath\":[\"/data/\"],"
                + "\"fileType\":[\"nc\"],"
                + "\"lastModified\":1000000,"
                + "\"domain\":1,\"fileSize\":1024,"
                + "\"globalAttributes\":[],\"variables\":[]}";
        mockMvc.perform(post("/api/metadata")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupDoc))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // Date-type globalAttribute, expect: true (Date value converted from long)
        String dateDoc = "{\"digestString\":\"newDigest003\","
                + "\"fileName\":[\"/data/date.nc\"],"
                + "\"filePath\":[\"/data/\"],"
                + "\"fileType\":[\"nc\"],"
                + "\"lastModified\":1000000,"
                + "\"domain\":0,\"fileSize\":512,"
                + "\"globalAttributes\":[{\"attributeName\":\"time\",\"type\":\"Date\",\"value\":2200000000}],"
                + "\"variables\":[]}";
        mockMvc.perform(post("/api/metadata")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dateDoc))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Same digestString different fileName → updates existing doc's fileName set, expect: false
        String sameDigestNewFile = "{\"digestString\":\"testDigest001\","
                + "\"fileName\":[\"/data/another.nc\"],"
                + "\"filePath\":[\"/data2/\"],"
                + "\"fileType\":[\"nc\"],"
                + "\"lastModified\":2000000,"
                + "\"domain\":1,\"fileSize\":1024,"
                + "\"globalAttributes\":[],\"variables\":[]}";
        mockMvc.perform(post("/api/metadata")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameDigestNewFile))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // removeFilenames: old doc with single fileName is removed when a new digest claims the same file, expect: true
        Metadata singleFileDoc = new Metadata();
        singleFileDoc.digestString = "oldDigest";
        singleFileDoc.fileName = new HashSet<>(Set.of("/data/removable.nc"));
        singleFileDoc.filePath = new HashSet<>(Set.of("/data/"));
        singleFileDoc.lastModified = System.currentTimeMillis();
        singleFileDoc.globalAttributes = new ArrayList<>();
        singleFileDoc.variables = new ArrayList<>();
        mongoTemplate.save(singleFileDoc, MongoCollections.METADATA);

        mockMvc.perform(post("/api/metadata")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"digestString\":\"newDigest3\","
                                + "\"fileName\":[\"/data/removable.nc\"],"
                                + "\"filePath\":[\"/data/\"],\"fileType\":[\"nc\"],"
                                + "\"lastModified\":1000000,\"domain\":0,\"fileSize\":512,"
                                + "\"globalAttributes\":[],\"variables\":[]}"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // removeFilenames: old doc with multiple fileNames has the matching name pulled, new doc inserted, expect: true
        Metadata multiFileDoc = new Metadata();
        multiFileDoc.digestString = "multiDigest";
        multiFileDoc.fileName = new HashSet<>(Set.of("/data/multi1.nc", "/data/multi2.nc"));
        multiFileDoc.filePath = new HashSet<>(Set.of("/data/"));
        multiFileDoc.lastModified = System.currentTimeMillis();
        multiFileDoc.globalAttributes = new ArrayList<>();
        multiFileDoc.variables = new ArrayList<>();
        mongoTemplate.save(multiFileDoc, MongoCollections.METADATA);

        mockMvc.perform(post("/api/metadata")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"digestString\":\"newDigest4\","
                                + "\"fileName\":[\"/data/multi1.nc\"],"
                                + "\"filePath\":[\"/data/\"],\"fileType\":[\"nc\"],"
                                + "\"lastModified\":1000000,\"domain\":0,\"fileSize\":512,"
                                + "\"globalAttributes\":[],\"variables\":[]}"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testFileNamesAndDeleteMetadata() throws Exception {
        // getFileNames, expect: list with at least our test entry
        mockMvc.perform(get("/api/metadata/filenames")
                        .param("limit", "10").param("offset", "0")
                        .with(user("u").roles("USER")))
                .andExpect(status().isOk());

        // deleteMetadata requires ADMIN
        mockMvc.perform(post("/api/metadata/filenames")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"/data/test.nc\"]"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/metadata/filenames")
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"/data/test.nc\"]"))
                .andExpect(status().isOk());

        // After deleting all docs, getFileNames returns empty list, expect: 200 with []
        mockMvc.perform(get("/api/metadata/filenames")
                        .param("limit", "10").param("offset", "0")
                        .with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void testGetDescriptions() throws Exception {
        mockMvc.perform(get("/api/metadata/description").with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables").exists())
                .andExpect(jsonPath("$.attributes").exists());
    }
}