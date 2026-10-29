package com.example.ungdungchiasecaycanh.ui.listing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.local.entity.ListingDraftEntity;
import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;

public class ListingDraftRoundTripTest {
    @Test public void everyFieldSurvivesSaveAndRestore() {
        CreateListingDraft draft = new CreateListingDraft();
        draft.setTransactionDetails(ListingType.TRADE, 0, "Sen đá");
        draft.setNickname("Lưỡi hổ");
        draft.setSpecies("Dracaena");
        draft.setDescription("Cây khoẻ");
        draft.setLocationText("Quận 3");
        draft.addImageUri("content://app/a.jpg");
        draft.addImageUri("content://app/b.jpg");
        draft.setCondition(ListingDetails.Condition.WITH_POT, true);
        draft.setPickupTime(ListingDetails.PickupTime.SUN_AFTERNOON, true);
        draft.setHeightCm(70);
        draft.setLight(LightLevel.SHADE);

        ListingDraftEntity saved = CreateListingViewModel.toEntity("uid-a", draft, 5L);
        CreateListingDraft back = new CreateListingDraft();
        CreateListingViewModel.apply(saved, back);

        assertEquals("uid-a", saved.getOwnerUid());
        assertEquals(ListingType.TRADE, back.getType());
        assertEquals("Sen đá", back.getLookingFor());
        assertEquals("Lưỡi hổ", back.getNickname());
        assertEquals("Quận 3", back.getLocationText());
        assertEquals(Arrays.asList("content://app/a.jpg", "content://app/b.jpg"), back.getImageUris());
        assertTrue(back.getDetails().getCondition().contains(ListingDetails.Condition.WITH_POT));
        assertTrue(back.getDetails().getPickupTimes().contains(ListingDetails.PickupTime.SUN_AFTERNOON));
        assertEquals(70, back.getDetails().getHeightCm());
        assertEquals(LightLevel.SHADE, back.getDetails().getLight());
    }

    @Test public void unknownStoredValuesAreSkipped() {
        ListingDraftEntity odd = new ListingDraftEntity("u", "SWAP", "", "", "", 0, "", "", "",
                "HEALTHY,FLYING", 0, "MOON", "SOMETIME", 1, "TREE", 0, "ANCIENT", false);
        CreateListingDraft back = new CreateListingDraft();
        CreateListingViewModel.apply(odd, back);
        assertEquals(ListingType.GIVE_AWAY, back.getType());
        assertEquals(Arrays.asList(ListingDetails.Condition.HEALTHY), back.getDetails().getCondition());
        assertEquals(null, back.getDetails().getLight());
        assertTrue(back.getDetails().getPickupTimes().isEmpty());
        assertEquals(null, back.getDetails().getCategory());
        assertEquals(null, back.getDetails().getAge());
    }

    @Test public void kindPotAgeAndNegotiableSurviveTheDraft() {
        CreateListingDraft d = new CreateListingDraft();
        d.setTransactionDetails(ListingType.SELL, 60000, "");
        d.setCategory(ListingDetails.Category.SUCCULENT);
        d.setPotCm(12);
        d.setAge(ListingDetails.Age.OVER_A_YEAR);
        d.setNegotiable(true);
        CreateListingDraft back = new CreateListingDraft();
        CreateListingViewModel.apply(CreateListingViewModel.toEntity("u", d, 1), back);
        assertEquals(ListingDetails.Category.SUCCULENT, back.getDetails().getCategory());
        assertEquals(12, back.getDetails().getPotCm());
        assertEquals(ListingDetails.Age.OVER_A_YEAR, back.getDetails().getAge());
        assertTrue(back.getDetails().isNegotiable());
    }

    @Test public void onlyASaleIsNegotiable() {
        CreateListingDraft d = new CreateListingDraft();
        d.setNegotiable(true);
        assertEquals(false, d.getDetails().isNegotiable());
    }
}
