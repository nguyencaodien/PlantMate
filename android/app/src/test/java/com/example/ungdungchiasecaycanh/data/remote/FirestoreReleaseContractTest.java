package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirestoreReleaseContractTest {
    @Test
    public void userBioAndImageFreeListingContractsArePresent() throws Exception {
        String rules = readRules();
        assertTrue(rules.contains("request.resource.data.bio.size() <= 500"));
        assertTrue(rules.contains("return images is list && images.size() <= 5"));
        assertFalse(rules.contains("images.size() >= 1"));
        assertTrue(rules.contains("image.matches('https?://.+')"));
    }

    @Test
    public void listingUpdatesRemainStatusOnly() throws Exception {
        String rules = readRules();
        assertTrue(rules.contains(".hasOnly(['status', 'updatedAt'])"));
    }

    private static String readRules() throws Exception {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path file = cwd.resolve("firestore.rules");
        if (!Files.exists(file)) file = cwd.resolve("../firestore.rules");
        if (!Files.exists(file)) file = cwd.resolve("../../firestore.rules");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
