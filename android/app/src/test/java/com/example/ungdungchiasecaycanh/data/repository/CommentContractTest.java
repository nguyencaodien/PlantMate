package com.example.ungdungchiasecaycanh.data.repository;
import static org.junit.Assert.*;import com.example.ungdungchiasecaycanh.model.Comment;import org.junit.Test;
public class CommentContractTest{
 @Test(expected=IllegalArgumentException.class)public void blankInvalid(){CommunityRepository.normalizeComment("u","","   ");}
 @Test public void oneAndThousandValid(){assertEquals("x",CommunityRepository.normalizeComment("u","","x").getText());assertEquals(1000,CommunityRepository.normalizeComment("u","",new String(new char[1000]).replace('\0','x')).getText().length());}
 @Test(expected=IllegalArgumentException.class)public void overThousandInvalid(){CommunityRepository.normalizeComment("u","",new String(new char[1001]).replace('\0','x'));}
 @Test public void replyContract(){Comment top=new Comment("c1","u","","x",null);Comment reply=new Comment("r1","u","c1","x",null);assertTrue(CommunityRepository.canReplyTo(top));assertFalse(CommunityRepository.canReplyTo(reply));assertFalse(CommunityRepository.canReplyTo(null));}
}
