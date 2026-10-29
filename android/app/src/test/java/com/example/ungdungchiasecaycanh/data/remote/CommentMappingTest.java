package com.example.ungdungchiasecaycanh.data.remote;
import static org.junit.Assert.*;import com.example.ungdungchiasecaycanh.model.Comment;import java.util.Map;import org.junit.Test;
public class CommentMappingTest{
 @Test public void topLevelMapHasFourFields(){Map<String,Object> m=FirebaseCommunityDataSource.commentCreateFields(new Comment(null,"u","","hello",null));assertEquals(4,m.size());assertEquals("",m.get("replyToCommentId"));assertFalse(m.containsKey("commentId"));assertFalse(m.containsKey("postId"));}
 @Test public void replyMapUsesParent(){Map<String,Object> m=FirebaseCommunityDataSource.commentCreateFields(new Comment(null,"u","c1","hello",null));assertEquals("c1",m.get("replyToCommentId"));}
}
