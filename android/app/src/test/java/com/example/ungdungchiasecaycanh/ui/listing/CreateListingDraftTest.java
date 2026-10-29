package com.example.ungdungchiasecaycanh.ui.listing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;

import java.util.Arrays;

import org.junit.Test;

public class CreateListingDraftTest {
    private CreateListingDraft validDraft() {
        CreateListingDraft draft = new CreateListingDraft();
        draft.addImageUri("content://cover");
        draft.setNickname("Mochi");
        draft.setSpecies("Monstera deliciosa");
        draft.setDescription("Healthy cutting");
        draft.setLocationText("District 7");
        return draft;
    }

    @Test
    public void zeroImagesIsValid() {
        CreateListingDraft draft = new CreateListingDraft();
        draft.setNickname("Mochi");
        draft.setDescription("Healthy cutting");
        draft.setLocationText("District 7");
        assertTrue(draft.hasValidImageCount());
        assertTrue(draft.validate().isValid());
    }

    @Test
    public void oneImageIsValid() {
        CreateListingDraft draft = new CreateListingDraft();
        assertTrue(draft.addImageUri("content://one"));
        assertTrue(draft.hasValidImageCount());
    }

    @Test
    public void fiveImagesAreValid() {
        CreateListingDraft draft = new CreateListingDraft();
        for (int i = 0; i < 5; i++) {
            assertTrue(draft.addImageUri("content://" + i));
        }
        assertEquals(5, draft.getImageUris().size());
        assertTrue(draft.hasValidImageCount());
    }

    @Test
    public void sixthImageIsRejected() {
        CreateListingDraft draft = new CreateListingDraft();
        for (int i = 0; i < 5; i++) {
            draft.addImageUri("content://" + i);
        }
        assertFalse(draft.addImageUri("content://six"));
        assertEquals(5, draft.getImageUris().size());
    }

    @Test
    public void removingImageUpdatesCountAndKeepsCoverOrder() {
        CreateListingDraft draft = new CreateListingDraft();
        draft.addImageUri("content://cover");
        draft.addImageUri("content://second");
        assertTrue(draft.removeImageAt(1));
        assertEquals(1, draft.getImageUris().size());
        assertEquals("content://cover", draft.getImageUris().get(0));
    }

    @Test
    public void blankNicknameAndDescriptionAreInvalid() {
        CreateListingDraft draft = validDraft();
        draft.setNickname(" ");
        draft.setDescription(" ");
        assertEquals(CreateListingDraft.Error.NICKNAME_REQUIRED, draft.validate().nickname);
        assertEquals(CreateListingDraft.Error.DESCRIPTION_REQUIRED, draft.validate().description);
    }

    @Test
    public void longNicknameSpeciesDescriptionAndLocationAreInvalid() {
        CreateListingDraft draft = validDraft();
        draft.setNickname(repeat('n', 61));
        draft.setSpecies(repeat('s', 101));
        draft.setDescription(repeat('d', 1001));
        draft.setLocationText(repeat('l', 101));
        CreateListingDraft.ValidationResult result = draft.validate();
        assertEquals(CreateListingDraft.Error.NICKNAME_TOO_LONG, result.nickname);
        assertEquals(CreateListingDraft.Error.SPECIES_TOO_LONG, result.species);
        assertEquals(CreateListingDraft.Error.DESCRIPTION_TOO_LONG, result.description);
        assertEquals(CreateListingDraft.Error.LOCATION_TOO_LONG, result.location);
    }

    @Test
    public void blankSpeciesIsValid() {
        CreateListingDraft draft = validDraft();
        draft.setSpecies(" ");
        assertEquals(CreateListingDraft.Error.NONE, draft.validate().species);
    }

    @Test
    public void giveAwayNormalizesPriceAndLookingFor() {
        CreateListingDraft draft = validDraft();
        draft.setTransactionDetails(ListingType.GIVE_AWAY, 100, "trade");
        assertEquals(0, draft.getPrice());
        assertEquals("", draft.getLookingFor());
        assertTrue(draft.validate().isValid());
    }

    @Test
    public void tradeRequiresLookingForAndNormalizesPrice() {
        CreateListingDraft draft = validDraft();
        draft.setTransactionDetails(ListingType.TRADE, 100, " ");
        assertEquals(CreateListingDraft.Error.LOOKING_FOR_REQUIRED,
                draft.validate().lookingFor);
        draft.setLookingFor(" Small pothos ");
        assertEquals(0, draft.getPrice());
        assertEquals("Small pothos", draft.getLookingFor());
        assertTrue(draft.validate().isValid());
    }

    @Test
    public void tradeLookingForTooLongIsInvalid() {
        CreateListingDraft draft = validDraft();
        draft.setTransactionDetails(ListingType.TRADE, 0, repeat('x', 201));
        assertEquals(CreateListingDraft.Error.LOOKING_FOR_TOO_LONG,
                draft.validate().lookingFor);
    }

    @Test
    public void sellRequiresPositivePriceAndClearsLookingFor() {
        CreateListingDraft draft = validDraft();
        draft.setTransactionDetails(ListingType.SELL, 0, "trade");
        assertEquals(CreateListingDraft.Error.SELL_PRICE_REQUIRED, draft.validate().price);
        draft.setPrice(-1);
        assertEquals(CreateListingDraft.Error.SELL_PRICE_REQUIRED, draft.validate().price);
        draft.setPrice(180000);
        assertEquals("", draft.getLookingFor());
        assertTrue(draft.validate().isValid());
    }

    @Test
    public void progressCountsTheSevenPartsAndDetailsAreOptional() {
        CreateListingDraft draft = new CreateListingDraft();
        assertEquals(7, draft.missingSteps().size());
        draft.setNickname("Mochi");
        draft.setCondition(ListingDetails.Condition.HEALTHY, true);
        draft.setCondition(ListingDetails.Condition.WITH_POT, true);
        draft.setCondition(ListingDetails.Condition.HEALTHY, false);
        draft.setHeightCm(70);
        draft.setLight(LightLevel.SHADE);
        assertEquals(Arrays.asList(CreateListingDraft.Step.PHOTOS, CreateListingDraft.Step.DESCRIPTION,
                CreateListingDraft.Step.PICKUP), draft.missingSteps());
        ListingDetails details = draft.getDetails();
        assertEquals(Arrays.asList(ListingDetails.Condition.WITH_POT), details.getCondition());
        assertEquals(70, details.getHeightCm());
        draft.setHeightCm(5000);
        assertEquals(ListingDetails.MAX_HEIGHT_CM, draft.getDetails().getHeightCm());
        // Details never block a valid listing.
        assertTrue(validDraft().getDetails().isEmpty());
        assertTrue(validDraft().validate().isValid());
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    /** The form survives process death through the saved state, which serializes it. */
    @Test public void draftSurvivesSerialization() throws Exception {
        CreateListingDraft draft = validDraft();
        draft.setCondition(ListingDetails.Condition.WITH_POT, true);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) { out.writeObject(draft); }
        CreateListingDraft back;
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(bytes.toByteArray()))) { back = (CreateListingDraft) in.readObject(); }
        assertEquals(draft.getNickname(), back.getNickname());
        assertEquals(draft.getType(), back.getType());
        assertEquals(draft.getImageUris(), back.getImageUris());
    }
}
