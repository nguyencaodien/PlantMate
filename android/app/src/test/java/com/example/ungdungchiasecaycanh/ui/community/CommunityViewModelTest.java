package com.example.ungdungchiasecaycanh.ui.community;

import static org.junit.Assert.*;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CommunityViewModelTest {
    private CommunityPostItem item(String id,String author,String text,PostCategory c){return new CommunityPostItem(new Post(id,"u",text,c,"","",null),author,false,0,false);}
    @Test public void categoryAndSearchCompose(){List<CommunityPostItem> source=Arrays.asList(item("1","Maya","Monstera help",PostCategory.QUESTION),item("2","Leo","Monstera win",PostCategory.PLANT_WIN));List<CommunityPostItem> result=CommunityViewModel.filter(source,PostCategory.QUESTION,"monstera");assertEquals(1,result.size());assertEquals("1",result.get(0).post.getPostId());assertEquals(2,source.size());}
    @Test public void authorSearchIsCaseInsensitive(){assertEquals(1,CommunityViewModel.filter(Arrays.asList(item("1","Maya Lin","hello",PostCategory.GENERAL)),null,"maya").size());}
    @Test public void allReturnsCopy(){List<CommunityPostItem> source=Arrays.asList(item("1","A","x",PostCategory.TIP));List<CommunityPostItem> result=CommunityViewModel.filter(source,null,"");assertEquals(1,result.size());assertNotSame(source,result);}
    @Test public void everyCategoryFilters(){List<CommunityPostItem> source=Arrays.asList(item("1","A","a",PostCategory.GENERAL),item("2","A","b",PostCategory.SHARING),item("3","A","c",PostCategory.QUESTION),item("4","A","d",PostCategory.PLANT_WIN),item("5","A","e",PostCategory.TIP));for(PostCategory category:PostCategory.values())assertEquals(1,CommunityViewModel.filter(source,category,"").size());}
    @Test public void likeSuccessChangesOnce(){CommunityPostItem before=item("1","A","x",PostCategory.GENERAL);CommunityPostItem after=CommunityViewModel.afterLikeOperation(before,true);assertTrue(after.liked);assertEquals(1,after.likeCount);}
    @Test public void unlikeNeverNegativeAndFailurePreserves(){CommunityPostItem liked=new CommunityPostItem(item("1","A","x",PostCategory.GENERAL).post,"A",true,0,false);CommunityPostItem after=CommunityViewModel.afterLikeOperation(liked,true);assertFalse(after.liked);assertEquals(0,after.likeCount);CommunityPostItem failed=CommunityViewModel.afterLikeOperation(liked,false);assertTrue(failed.liked);assertEquals(0,failed.likeCount);}
}
