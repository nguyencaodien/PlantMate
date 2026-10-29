package com.example.ungdungchiasecaycanh.model;
import static org.junit.Assert.*;
import org.junit.Test;
public class PostCategoryTest {
 @Test public void canonicalValuesRoundTrip(){for(PostCategory category:PostCategory.values())assertEquals(category,PostCategory.fromFirestore(category.name()));}
 @Test public void invalidValueIsSafe(){assertNull(PostCategory.fromFirestore("NEWS"));assertNull(PostCategory.fromFirestore(null));}
}
