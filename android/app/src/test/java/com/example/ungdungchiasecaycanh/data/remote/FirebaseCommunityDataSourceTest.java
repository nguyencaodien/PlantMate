package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.*;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import java.util.Map;
import org.junit.Test;

public class FirebaseCommunityDataSourceTest {
    @Test public void postMapHasExactlyCanonicalFields(){Map<String,Object> m=FirebaseCommunityDataSource.postCreateFields(new Post(null,"u","t",PostCategory.GENERAL,"","",null));assertEquals(6,m.size());assertFalse(m.containsKey("postId"));assertEquals("",m.get("imageUrl"));}
    @Test public void likeMapHasOnlyCreatedAt(){Map<String,Object> m=FirebaseCommunityDataSource.likeCreateFields();assertEquals(1,m.size());assertTrue(m.containsKey("createdAt"));}
}
