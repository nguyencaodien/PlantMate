package com.example.ungdungchiasecaycanh.ui.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Map;

public class AdminValidationTest {

    @Test public void validGiveAwayListingHasNoErrors() {
        Map<String, Integer> errors = AdminValidation.listing("Mochi", "Snake plant", "Healthy pup",
                ListingType.GIVE_AWAY, "", "", "Quận 7");
        assertTrue(errors.isEmpty());
    }

    @Test public void sellNeedsPositivePriceAndTradeNeedsLookingFor() {
        Map<String, Integer> sell = AdminValidation.listing("Mochi", "", "Nice", ListingType.SELL, "0", "", "Q7");
        assertEquals(Integer.valueOf(R.string.admin_error_price), sell.get(AdminValidation.PRICE));
        Map<String, Integer> trade = AdminValidation.listing("Mochi", "", "Nice", ListingType.TRADE, "", " ", "Q7");
        assertEquals(Integer.valueOf(R.string.admin_error_required), trade.get(AdminValidation.LOOKING_FOR));
    }

    @Test public void listingLimitsMatchTheRules() {
        String longName = new String(new char[61]).replace('\0', 'a');
        Map<String, Integer> errors = AdminValidation.listing(longName, "", "", ListingType.GIVE_AWAY, "", "", "");
        assertEquals(Integer.valueOf(R.string.admin_error_too_long_60), errors.get(AdminValidation.NICKNAME));
        assertEquals(Integer.valueOf(R.string.admin_error_required), errors.get(AdminValidation.DESCRIPTION));
        assertEquals(Integer.valueOf(R.string.admin_error_required), errors.get(AdminValidation.LOCATION));
    }

    @Test public void priceParsingAcceptsVietnameseGrouping() {
        assertEquals(180000, AdminValidation.parsePrice("180.000"));
        assertEquals(-1, AdminValidation.parsePrice("abc"));
        assertEquals(-1, AdminValidation.parsePrice(""));
    }

    @Test public void postTextIsRequiredAndBounded() {
        assertEquals(R.string.admin_error_required, AdminValidation.postText("   "));
        assertEquals(0, AdminValidation.postText("Is a ZZ plant safe for cats?"));
        assertEquals(R.string.admin_error_too_long_2000,
                AdminValidation.postText(new String(new char[2001]).replace('\0', 'x')));
    }

    @Test public void memberNeedsNameAndArea() {
        Map<String, Integer> errors = AdminValidation.member(" ", "", "");
        assertEquals(Integer.valueOf(R.string.admin_error_required), errors.get(AdminValidation.DISPLAY_NAME));
        assertEquals(Integer.valueOf(R.string.admin_error_required), errors.get(AdminValidation.LOCATION));
        assertTrue(AdminValidation.member("Linh", "Quận 7", "").isEmpty());
    }

    @Test public void searchIgnoresCaseAndVietnameseAccents() {
        assertTrue(AdminValidation.matches("quan 7", "Quận 7, TP.HCM"));
        assertTrue(AdminValidation.matches("HO TRONG", "Hồ Trọng Phúc"));
        assertTrue(AdminValidation.matches("", "anything"));
        assertFalse(AdminValidation.matches("orchid", "Snake plant", "Mochi"));
    }
}
