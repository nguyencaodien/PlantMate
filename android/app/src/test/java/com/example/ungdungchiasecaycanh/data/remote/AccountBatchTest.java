package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Deletion batches stay under Firestore's 500 writes. */
public class AccountBatchTest {
    @Test public void refsAreSplitIntoBatchesOfAtMost400() {
        List<Integer> refs = new ArrayList<>();
        for (int i = 0; i < 901; i++) refs.add(i);
        List<List<Integer>> batches = FirebaseAccountDataSource.chunks(refs, FirebaseAccountDataSource.BATCH_SIZE);
        assertEquals(3, batches.size());
        assertEquals(400, batches.get(0).size());
        assertEquals(101, batches.get(2).size());
        assertEquals(0, FirebaseAccountDataSource.chunks(new ArrayList<Integer>(), 400).size());
    }
}
