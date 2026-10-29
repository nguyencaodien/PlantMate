package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assume.assumeTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Account deletion dry run against the real project, as the signed-in member: runs every query the
 * deletion runs (proving the deployed rules and the comments.authorId collection-group index) and
 * deletes nothing. Counts per collection go to logcat under "DeletionDryRun".
 */
@RunWith(AndroidJUnit4.class)
public class AccountDeletionDryRunTest {
    @Test
    public void everyDeletionQueryIsAllowed() throws Exception {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assumeTrue("Sign in on the device first", user != null);
        List<DocumentReference> refs = Tasks.await(
                new FirebaseAccountDataSource().findMemberData(user.getUid()), 60, TimeUnit.SECONDS);
        Map<String, Integer> counts = new TreeMap<>();
        for (DocumentReference ref : refs) {
            String[] parts = ref.getPath().split("/");
            String key = parts.length > 2 ? parts[0] + "/*/" + parts[parts.length - 2] : parts[0];
            counts.merge(key, 1, Integer::sum);
        }
        Log.i("DeletionDryRun", refs.size() + " documents would be deleted (plus users/{uid}): " + counts);
    }
}
