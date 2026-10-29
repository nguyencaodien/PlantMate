package com.example.ungdungchiasecaycanh.ui.community;
import static org.junit.Assert.*;import com.example.ungdungchiasecaycanh.model.*;import java.util.Collections;import org.junit.Test;
public class ShareListingContractTest{
 private Listing listing(ListingStatus s){return new Listing("l","o","Plant","","d",ListingType.GIVE_AWAY,0,"",Collections.singletonList("https://valid"),"Area",s,null,null);}
 @Test public void onlyActiveCanShare(){assertTrue(CreatePostViewModel.canShare(listing(ListingStatus.ACTIVE)));assertFalse(CreatePostViewModel.canShare(listing(ListingStatus.RESERVED)));assertFalse(CreatePostViewModel.canShare(listing(ListingStatus.REHOMED)));assertFalse(CreatePostViewModel.canShare(listing(ListingStatus.UNAVAILABLE)));}
}
