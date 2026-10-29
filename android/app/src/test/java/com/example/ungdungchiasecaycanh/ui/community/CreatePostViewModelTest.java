package com.example.ungdungchiasecaycanh.ui.community;
import static org.junit.Assert.*;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import org.junit.Test;
public class CreatePostViewModelTest {
 @Test public void blankInvalid(){assertNotNull(CreatePostViewModel.validate("  ",PostCategory.GENERAL));}
 @Test public void oneCharacterValid(){assertNull(CreatePostViewModel.validate("x",PostCategory.GENERAL));}
 @Test public void categoryRequired(){assertNotNull(CreatePostViewModel.validate("hello",null));}
 @Test public void maxValid(){assertNull(CreatePostViewModel.validate(new String(new char[2000]).replace('\0','x'),PostCategory.TIP));}
 @Test public void overMaxInvalid(){assertNotNull(CreatePostViewModel.validate(new String(new char[2001]).replace('\0','x'),PostCategory.TIP));}
}
