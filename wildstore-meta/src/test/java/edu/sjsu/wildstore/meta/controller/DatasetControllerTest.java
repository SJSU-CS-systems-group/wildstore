package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.Dataset;
import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.meta.MongoCollections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import edu.sjsu.wildstore.WildfireAttribute;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DatasetControllerTest extends BaseControllerTest {

    @Autowired
    private DatasetController datasetController;

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().getCollection("dataset").drop();
        mongoTemplate.getDb().getCollection("metadata").drop();
    }

    @Test
    void testEncodingBase64() {
        // Empty string decodes to BigInteger.ZERO
        assertEquals(BigInteger.ZERO, DatasetController.base64decoding(""));

        // Valid base64 string decodes to a non-zero BigInteger
        byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        String encoded = Base64.getUrlEncoder().encodeToString(bytes);
        BigInteger decoded = DatasetController.base64decoding(encoded);
        assertNotNull(decoded);
        assertNotEquals(BigInteger.ZERO, decoded);

        // Encoding produces exactly 16 characters (12 bytes, exactly 16 base64 chars)
        BigInteger value = new BigInteger(1, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        String result = DatasetController.base64encoding(value);
        assertEquals(16, result.length());

        // Two different large values, expect: two different encodings (values must encode to ≥16 chars)
        BigInteger a = new BigInteger(1, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        BigInteger b = new BigInteger(1, new byte[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13});
        assertNotEquals(DatasetController.base64encoding(a), DatasetController.base64encoding(b));
    }

    @Test
    void testUpsertDataset() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(post("/api/dataset"))
                .andExpect(status().isUnauthorized());

        // Authenticated USER with empty DB, expect: 200 OK (also creates DatasetCreation.log)
        mockMvc.perform(post("/api/dataset").with(user("testuser").roles("USER")))
                .andExpect(status().isOk());

        // Insert metadata with filePath and digestString, then call upsertDataset, expect: dataset doc created
        Metadata m = new Metadata();
        m.digestString = "testDigest001";
        m.fileName = new HashSet<>(Set.of("/data/file1.nc"));
        m.filePath = new HashSet<>(Set.of("/data/"));
        m.domain = 0;
        m.lastModified = System.currentTimeMillis();
        m.globalAttributes = new ArrayList<>();
        m.variables = new ArrayList<>();
        mongoTemplate.save(m, MongoCollections.METADATA);

        mockMvc.perform(post("/api/dataset").with(user("testuser").roles("USER")))
                .andExpect(status().isOk());
        List<Dataset> datasets = mongoTemplate.findAll(Dataset.class, "dataset");
        assertFalse(datasets.isEmpty());

        mongoTemplate.getDb().getCollection("dataset").drop();

        // upsertDatasetPath: new digestString, expect: creates doc
        Dataset dataset = new Dataset();
        dataset.digestString = "upDigest001";
        dataset.datasetPath = new HashSet<>(Set.of("/uppath/"));
        dataset.digestList = new ArrayList<>(List.of("upDigest001"));
        dataset.maxDomain = 0;
        datasetController.upsertDatasetPath(dataset, "/uppath/");
        Dataset saved = mongoTemplate.findOne(
                new Query(Criteria.where("digestString").is("upDigest001")),
                Dataset.class, "dataset");
        assertNotNull(saved);
        assertTrue(saved.datasetPath.contains("/uppath/"));

        // upsertDatasetPath: existing digestString, expect: adds new path to existing doc
        datasetController.upsertDatasetPath(dataset, "/uppath2/");
        Dataset updatedDoc = mongoTemplate.findOne(
                new Query(Criteria.where("digestString").is("upDigest001")),
                Dataset.class, "dataset");
        assertNotNull(updatedDoc);
        assertTrue(updatedDoc.datasetPath.contains("/uppath2/"));

        mongoTemplate.getDb().getCollection("dataset").drop();

        // checkUpdate: new path (no existing doc), expect: calls upsertDatasetPath, expect: creates doc
        Dataset ckDataset = new Dataset();
        ckDataset.digestString = "ckDigest001";
        ckDataset.datasetPath = new HashSet<>(Set.of("/ckpath/"));
        ckDataset.digestList = new ArrayList<>(List.of("ckDigest001"));
        ckDataset.maxDomain = 0;
        datasetController.checkUpdate(ckDataset, "/ckpath/");
        Dataset ckSaved = mongoTemplate.findOne(
                new Query(Criteria.where("digestString").is("ckDigest001")),
                Dataset.class, "dataset");
        assertNotNull(ckSaved);

        // checkUpdate: same digest, expect: no-op (doc unchanged)
        datasetController.checkUpdate(ckDataset, "/ckpath/");

        // checkUpdate: different digest, single path, expect: removes old doc, creates new
        Dataset oldDataset = new Dataset();
        oldDataset.digestString = "ckOldDigest";
        oldDataset.datasetPath = new HashSet<>(Set.of("/ckpath2/"));
        oldDataset.digestList = new ArrayList<>(List.of("ckOldDigest"));
        oldDataset.maxDomain = 0;
        mongoTemplate.save(oldDataset, "dataset");

        Dataset newDataset = new Dataset();
        newDataset.digestString = "ckNewDigest";
        newDataset.datasetPath = new HashSet<>(Set.of("/ckpath2/"));
        newDataset.digestList = new ArrayList<>(List.of("ckNewDigest"));
        newDataset.maxDomain = 0;

        datasetController.checkUpdate(newDataset, "/ckpath2/");
        List<Dataset> oldDocs = mongoTemplate.find(
                new Query(Criteria.where("digestString").is("ckOldDigest")),
                Dataset.class, "dataset");
        assertTrue(oldDocs.isEmpty());
        List<Dataset> newDocs = mongoTemplate.find(
                new Query(Criteria.where("digestString").is("ckNewDigest")),
                Dataset.class, "dataset");
        assertFalse(newDocs.isEmpty());

        mongoTemplate.getDb().getCollection("dataset").drop();

        // checkUpdate: multi-path dataset, digest changes for one path — path removed but doc NOT deleted
        Dataset multiPathDataset = new Dataset();
        multiPathDataset.digestString = "mpOldDigest";
        multiPathDataset.datasetPath = new HashSet<>(Set.of("/mppath1/", "/mppath2/"));
        multiPathDataset.digestList = new ArrayList<>(List.of("mpOldDigest"));
        multiPathDataset.maxDomain = 0;
        mongoTemplate.save(multiPathDataset, "dataset");

        Dataset mpNewDataset = new Dataset();
        mpNewDataset.digestString = "mpNewDigest";
        mpNewDataset.datasetPath = new HashSet<>(Set.of("/mppath1/"));
        mpNewDataset.digestList = new ArrayList<>(List.of("mpNewDigest"));
        mpNewDataset.maxDomain = 0;

        datasetController.checkUpdate(mpNewDataset, "/mppath1/");
        Dataset mpRemaining = mongoTemplate.findOne(
                new Query(Criteria.where("digestString").is("mpOldDigest")),
                Dataset.class, "dataset");
        assertNotNull(mpRemaining);
        assertFalse(mpRemaining.datasetPath.contains("/mppath1/"));
        assertTrue(mpRemaining.datasetPath.contains("/mppath2/"));

        mongoTemplate.getDb().getCollection("dataset").drop();
        mongoTemplate.getDb().getCollection("metadata").drop();

        // Two metadata docs with same filePath
        // Digest strings must be valid base64url (16 chars) so base64decoding doesn't throw
        String d1 = DatasetController.base64encoding(new BigInteger(1, new byte[]{1,2,3,4,5,6,7,8,9,10,11,12}));
        String d2 = DatasetController.base64encoding(new BigInteger(1, new byte[]{13,14,15,16,17,18,19,20,21,22,23,24}));

        WildfireAttribute startDate1 = new WildfireAttribute();
        startDate1.attributeName = "StartDate";
        startDate1.type = "date";
        startDate1.value = new java.util.Date(1000000L);

        Metadata m1 = new Metadata();
        m1.digestString = d1;
        m1.fileName = new HashSet<>(Set.of("/data/shared/file1.nc"));
        m1.filePath = new HashSet<>(Set.of("/data/shared/"));
        m1.domain = 1;
        m1.lastModified = System.currentTimeMillis();
        m1.globalAttributes = new ArrayList<>(List.of(startDate1));
        m1.variables = new ArrayList<>();

        WildfireAttribute startDate2 = new WildfireAttribute();
        startDate2.attributeName = "StartDate";
        startDate2.type = "date";
        startDate2.value = new java.util.Date(2000000L);

        Metadata m2 = new Metadata();
        m2.digestString = d2;
        m2.fileName = new HashSet<>(Set.of("/data/shared/file2.nc"));
        m2.filePath = new HashSet<>(Set.of("/data/shared/"));
        m2.domain = 2;
        m2.lastModified = System.currentTimeMillis();
        m2.globalAttributes = new ArrayList<>(List.of(startDate2));
        m2.variables = new ArrayList<>();

        mongoTemplate.save(m1, MongoCollections.METADATA);
        mongoTemplate.save(m2, MongoCollections.METADATA);

        mockMvc.perform(post("/api/dataset").with(user("testuser").roles("USER")))
                .andExpect(status().isOk());

        List<Dataset> elseDatasets = mongoTemplate.findAll(Dataset.class, "dataset");
        assertFalse(elseDatasets.isEmpty());
        Dataset combined = elseDatasets.get(0);
        assertEquals(2, combined.maxDomain);
        assertTrue(combined.digestList.contains(d1) || combined.digestList.contains(d2));
    }
}