package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.*;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SafetyRepositoryTest {
    @Test public void canonicalEnumsOnly(){
        assertArrayEquals(new String[]{"USER","LISTING","POST","COMMENT"},
                Arrays.stream(ReportTargetType.values()).map(Enum::name).toArray(String[]::new));
        assertArrayEquals(new String[]{"HARASSMENT","SPAM","SCAM","INAPPROPRIATE","OTHER"},
                Arrays.stream(ReportReason.values()).map(Enum::name).toArray(String[]::new));
    }
    @Test public void detailsNormalizeAndBounds(){
        Report value=SafetyRepository.normalizeReport("reporter",ReportTargetType.POST,"post","",
                ReportReason.SPAM,"  reason  ");
        assertEquals("reason",value.getDetails());
        assertEquals("",SafetyRepository.normalizeReport("reporter",ReportTargetType.USER,"target","",
                ReportReason.SPAM,"  ").getDetails());
        assertEquals(500,SafetyRepository.normalizeReport("reporter",ReportTargetType.USER,"target","",
                ReportReason.OTHER,"x".repeat(500)).getDetails().length());
        assertThrows(IllegalArgumentException.class,()->SafetyRepository.normalizeReport("reporter",
                ReportTargetType.USER,"target","",ReportReason.OTHER," \n "));
        assertThrows(IllegalArgumentException.class,()->SafetyRepository.normalizeReport("reporter",
                ReportTargetType.USER,"target","",ReportReason.SPAM,"x".repeat(501)));
    }
    @Test public void commentRequiresParentAndOthersRejectParent(){
        for(ReportTargetType type:ReportTargetType.values()){
            if(type==ReportTargetType.COMMENT){
                assertThrows(IllegalArgumentException.class,()->SafetyRepository.normalizeReport("r",type,"c"," ",ReportReason.SPAM,""));
                assertEquals("post",SafetyRepository.normalizeReport("r",type,"c"," post ",ReportReason.SPAM,"").getParentId());
            }else{
                assertEquals("",SafetyRepository.normalizeReport("r",type,"id","",ReportReason.SPAM,"").getParentId());
                assertThrows(IllegalArgumentException.class,()->SafetyRepository.normalizeReport("r",type,"id","post",ReportReason.SPAM,""));
            }
        }
    }
    @Test public void selfTargetsRejectedAcrossTargetKinds(){
        for(ReportTargetType type:ReportTargetType.values()){
            assertTrue(SafetyRepository.isSelfTarget("A","A"));
            assertFalse(SafetyRepository.isSelfTarget("A","B"));
        }
    }
    @Test public void listingAndPostFilteringPreserveSource(){
        List<Listing> listings=Arrays.asList(listing("A"),listing("B"),listing("C"));
        List<Listing> visible=SafetyRepository.filterByBlockedAuthors(listings,
                Collections.singleton("B"),Listing::getOwnerId);
        assertEquals(Arrays.asList("A","C"),Arrays.asList(visible.get(0).getOwnerId(),visible.get(1).getOwnerId()));
        assertEquals(3,listings.size());
        List<Post> posts=Arrays.asList(post("A"),post("B"),post("C"));
        List<Post> filtered=SafetyRepository.filterByBlockedAuthors(posts,
                Collections.singleton("B"),Post::getAuthorId);
        assertEquals(2,filtered.size());assertEquals("A",filtered.get(0).getAuthorId());
        assertEquals("C",filtered.get(1).getAuthorId());assertEquals(3,posts.size());
    }
    @Test public void blockedParentHidesEntireReplySubtree(){
        List<Comment> source=Arrays.asList(comment("C1","B",""),comment("R1","A","C1"),
                comment("C2","A",""),comment("R2","B","C2"),comment("R3","C","C2"));
        List<Comment> visible=SafetyRepository.filterCommentTree(source,new HashSet<>(Collections.singleton("B")));
        assertEquals(2,visible.size());assertEquals("C2",visible.get(0).getCommentId());
        assertEquals("R3",visible.get(1).getCommentId());assertEquals(5,source.size());
    }
    private static Listing listing(String uid){return new Listing("id-"+uid,uid,"Plant","","Description",
            ListingType.GIVE_AWAY,0,"",Collections.singletonList("https://example.test/image"),"Area",
            ListingStatus.ACTIVE,null,null);}
    private static Post post(String uid){return new Post("id-"+uid,uid,"text",PostCategory.GENERAL,"","",null);}
    private static Comment comment(String id,String uid,String parent){return new Comment(id,uid,parent,"text",null);}
}
