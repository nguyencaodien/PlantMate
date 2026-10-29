package com.example.ungdungchiasecaycanh.debug;

import com.example.ungdungchiasecaycanh.model.LightLevel;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.CommunityRepository;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.data.repository.ImageRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.ui.auth.ForgotPasswordActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collections;
import java.util.List;

/**
 * Debug-only demo data seeder (not packaged in release builds). It publishes sample listings
 * and community posts as the signed-in user through the normal repositories, so every
 * repository validation and Firestore rule still applies. Images are hotlinked Wikimedia
 * Commons files; attribution lives in docs/sample_images/ATTRIBUTION.md.
 *
 * Run: adb shell am start -n com.example.ungdungchiasecaycanh/.debug.SampleDataActivity
 *   --es mode photo_posts : community posts with a hosted image (needs the updated rules)
 *   --es mode garden      : local Garden plants whose photos are saved to Pictures/PlantMate
 *   --es mode open_forgot : opens the Reset password screen without signing out (UI checks only)
 *   --es mode upload_post : uploads the newest Pictures/PlantMate photo through ImageRepository
 *       (compressed, stored in Firestore images/) and posts it (checks the member upload path)
 *   --es mode exchange_ask     : as the requester, message the owners of sample listings to ask
 *   --es mode exchange_rehome  : as an owner, mark asked-for sample listings REHOMED and review requesters
 *   --es mode exchange_review  : as a requester, review the owners of REHOMED listings you asked for
 *   --es mode persona --es persona minhanh|giahuy|thaovy : the signed-in account becomes that
 *       sample member (profile, 7 listings, posts, comments and likes on others' posts, Garden)
 */
public class SampleDataActivity extends AppCompatActivity {
    private static final String TAG = "PlantMateSeeder";
    private static final String COMMONS = "https://thumb.wikimedia.org/wikipedia/commons/thumb/";

    private final ListingRepository listings = new ListingRepository();
    private final CommunityRepository community = new CommunityRepository();
    private TextView log;

    private static final class SampleListing {
        final String nickname, species, description, lookingFor, location, image;
        final ListingType type;
        final long price;

        SampleListing(ListingType type, String nickname, String species, String description,
                      long price, String lookingFor, String location, String image) {
            this.type = type; this.nickname = nickname; this.species = species;
            this.description = description; this.price = price; this.lookingFor = lookingFor;
            this.location = location; this.image = image;
        }
    }

    private static final class SamplePost {
        final PostCategory category;
        final String text;
        final int sharedListingIndex;

        SamplePost(PostCategory category, String text, int sharedListingIndex) {
            this.category = category; this.text = text; this.sharedListingIndex = sharedListingIndex;
        }
    }

    private static final String IMG_SNAKE = "https://upload.wikimedia.org/wikipedia/commons/b/bc/Sansevieria_in_pot.jpg";
    private static final String IMG_SPIDER = COMMONS + "5/50/Chlorophytum_comosum_plant%2C_April_2023.jpg/960px-Chlorophytum_comosum_plant%2C_April_2023.jpg";
    private static final String IMG_MINT = COMMONS + "1/16/Mentha_spicata_var._viridis_kz01.jpg/960px-Mentha_spicata_var._viridis_kz01.jpg";
    private static final String IMG_ALOE = COMMONS + "d/d9/Aloe_Vera_Plant.JPG/960px-Aloe_Vera_Plant.JPG";
    private static final String IMG_LILY = COMMONS + "b/bc/A_spathiphyllum_01.jpg/960px-A_spathiphyllum_01.jpg";
    private static final String IMG_ZZ = COMMONS + "c/cf/Zamioculcas_zamiifolia_1.jpg/960px-Zamioculcas_zamiifolia_1.jpg";
    private static final String IMG_JADE = COMMONS + "5/5c/Crassula_ovata_%2B_Florero.jpg/960px-Crassula_ovata_%2B_Florero.jpg";
    private static final String IMG_FIG = COMMONS + "6/60/Ficus_lyrata_98162192.jpg/960px-Ficus_lyrata_98162192.jpg";
    private static final String IMG_ORCHID = COMMONS + "1/11/Pink_orchid_in_a_white_pot.jpg/960px-Pink_orchid_in_a_white_pot.jpg";

    private static final class PhotoPost {
        final PostCategory category;
        final String text, image;

        PhotoPost(PostCategory category, String text, String image) {
            this.category = category; this.text = text; this.image = image;
        }
    }

    private static final PhotoPost[] PHOTO_POSTS = {
        new PhotoPost(PostCategory.PLANT_WIN, "My spider plant finally sent out babies along the garden wall.", IMG_SPIDER),
        new PhotoPost(PostCategory.TIP, "Aloe wants bright light and very little water. Mine lives in the sunniest spot and gets watered about every ten days.", IMG_ALOE),
        new PhotoPost(PostCategory.PLANT_WIN, "Pink blooms again! Second flowering this year for this phalaenopsis.", IMG_ORCHID),
        new PhotoPost(PostCategory.QUESTION, "My peace lily droops every afternoon and recovers at night. Is it thirsty or just the heat?", IMG_LILY),
        new PhotoPost(PostCategory.TIP, "Snake plants cope with low light, but they grow much faster next to a bright window.", IMG_SNAKE),
        new PhotoPost(PostCategory.QUESTION, "Picked up this jade at a weekend market. Any tips to keep it compact?", IMG_JADE),
        new PhotoPost(PostCategory.PLANT_WIN, "New shoots on the ZZ plant after months of nothing.", IMG_ZZ),
        new PhotoPost(PostCategory.GENERAL, "The mint corner is flowering and the bees love it.", IMG_MINT),
        new PhotoPost(PostCategory.QUESTION, "My fiddle leaf fig keeps dropping its lower leaves. Should I move it away from the window?", IMG_FIG),
    };

    private static final class GardenPlant {
        final String nickname, species, image, note;
        final int interval;
        final CareType care;

        GardenPlant(String nickname, String species, String image, int interval, CareType care, String note) {
            this.nickname = nickname; this.species = species; this.image = image;
            this.interval = interval; this.care = care; this.note = note;
        }
    }

    private static final String[] SAMPLE_ROOMS = {"Phòng khách", "Ban công", "Bàn làm việc"};
    private static final LightLevel[] SAMPLE_LIGHTS = {LightLevel.BRIGHT_INDIRECT, LightLevel.SUN, LightLevel.SHADE};

    private static final GardenPlant[] GARDEN = {
        new GardenPlant("Spike", "Dracaena trifasciata", IMG_SNAKE, 14, null, null),
        new GardenPlant("Sunny", "Aloe vera", IMG_ALOE, 10, CareType.WATER, null),
        new GardenPlant("Lily", "Spathiphyllum wallisii", IMG_LILY, 5, CareType.WATER, null),
        new GardenPlant("Zed", "Zamioculcas zamiifolia", IMG_ZZ, 14, null, null),
        new GardenPlant("Penny", "Crassula ovata", IMG_JADE, 12, CareType.PRUNE, null),
        new GardenPlant("Pinky", "Phalaenopsis", IMG_ORCHID, 7, CareType.NOTE, "A second flower spike appeared."),
        new GardenPlant("Minty", "Mentha spicata", IMG_MINT, 2, CareType.WATER, null),
        new GardenPlant("Babies", "Chlorophytum comosum", IMG_SPIDER, 4, CareType.REPOT, null),
    };

    private static final SampleListing[] LISTINGS = {
        new SampleListing(ListingType.GIVE_AWAY, "Monstera cutting", "Monstera deliciosa",
                "Rooted cutting with two leaves and an aerial root. Pick up in the evening.",
                0, "", "Tan Phu, HCM", null),
        new SampleListing(ListingType.GIVE_AWAY, "Snake plant pups", "Dracaena trifasciata",
                "Three pups separated from my big pot last week. Very easy for beginners.",
                0, "", "Go Vap, HCM",
                "https://upload.wikimedia.org/wikipedia/commons/b/bc/Sansevieria_in_pot.jpg"),
        new SampleListing(ListingType.GIVE_AWAY, "Spider plant babies", "Chlorophytum comosum",
                "Plenty of baby plantlets with roots. Bring a small cup or bag.",
                0, "", "Binh Thanh, HCM",
                COMMONS + "5/50/Chlorophytum_comosum_plant%2C_April_2023.jpg/960px-Chlorophytum_comosum_plant%2C_April_2023.jpg"),
        new SampleListing(ListingType.GIVE_AWAY, "Mint runners", "Mentha spicata",
                "Mint is taking over my balcony. Happy to share a handful of rooted runners.",
                0, "", "District 3, HCM",
                COMMONS + "1/16/Mentha_spicata_var._viridis_kz01.jpg/960px-Mentha_spicata_var._viridis_kz01.jpg"),
        new SampleListing(ListingType.GIVE_AWAY, "Aloe vera pups", "Aloe vera",
                "Healthy pups from an older plant. Good for a sunny window.",
                0, "", "Thu Duc, HCM",
                COMMONS + "d/d9/Aloe_Vera_Plant.JPG/960px-Aloe_Vera_Plant.JPG"),
        new SampleListing(ListingType.TRADE, "Peace lily division", "Spathiphyllum wallisii",
                "Split from a mature plant that is already flowering again.",
                0, "Any hoya cutting", "Phu Nhuan, HCM",
                COMMONS + "b/bc/A_spathiphyllum_01.jpg/960px-A_spathiphyllum_01.jpg"),
        new SampleListing(ListingType.TRADE, "ZZ plant division", "Zamioculcas zamiifolia",
                "Small division with two stems. Fine in low light.",
                0, "Calathea or maranta", "District 7, HCM",
                COMMONS + "c/cf/Zamioculcas_zamiifolia_1.jpg/960px-Zamioculcas_zamiifolia_1.jpg"),
        new SampleListing(ListingType.TRADE, "Jade plant", "Crassula ovata",
                "Compact jade plant. The pot stays with me.",
                0, "String of pearls cutting", "Tan Binh, HCM",
                COMMONS + "5/5c/Crassula_ovata_%2B_Florero.jpg/960px-Crassula_ovata_%2B_Florero.jpg"),
        new SampleListing(ListingType.SELL, "Fiddle leaf fig", "Ficus lyrata",
                "About 80 cm tall. Moving house and cannot take it with me.",
                250000, "", "District 1, HCM",
                COMMONS + "6/60/Ficus_lyrata_98162192.jpg/960px-Ficus_lyrata_98162192.jpg"),
        new SampleListing(ListingType.SELL, "Pink phalaenopsis orchid", "Phalaenopsis",
                "Blooming now and comes in its white pot.",
                180000, "", "Tan Phu, HCM",
                COMMONS + "1/11/Pink_orchid_in_a_white_pot.jpg/960px-Pink_orchid_in_a_white_pot.jpg"),
    };

    private static final SamplePost[] POSTS = {
        new SamplePost(PostCategory.QUESTION, "My pothos leaves are turning yellow at the tips. I water once a week. Too much water, or not enough light?", -1),
        new SamplePost(PostCategory.TIP, "Water succulents only when the soil is completely dry. In the rainy season that can mean every 10 to 14 days.", -1),
        new SamplePost(PostCategory.PLANT_WIN, "First new leaf on my fiddle leaf fig since I moved it next to the east window!", -1),
        new SamplePost(PostCategory.SHARING, "Sharing snake plant pups from my pot. Free for anyone nearby who is just starting out.", 1),
        new SamplePost(PostCategory.QUESTION, "Has anyone dealt with fungus gnats? They appeared right after I repotted with new soil.", -1),
        new SamplePost(PostCategory.TIP, "Bottom watering saved my calathea: 20 minutes in a tray of water, then let it drain fully.", -1),
        new SamplePost(PostCategory.GENERAL, "Started a small balcony garden this month. Mint, basil and one very stubborn aloe.", -1),
        new SamplePost(PostCategory.SHARING, "Mint is taking over again. Rooted runners are free if you want to start a herb pot.", 3),
        new SamplePost(PostCategory.PLANT_WIN, "My orchid bloomed again after a whole year of waiting. Patience pays off.", -1),
        new SamplePost(PostCategory.QUESTION, "What potting mix works best for a monstera in a humid apartment?", -1),
        new SamplePost(PostCategory.TIP, "Turn your pots a quarter turn each week so plants grow straight instead of leaning toward the window.", -1),
        new SamplePost(PostCategory.SHARING, "Offering a peace lily division for trade. Looking for any hoya cutting.", 5),
        new SamplePost(PostCategory.GENERAL, "Who else waters early in the morning before the heat? Share your routine.", -1),
        new SamplePost(PostCategory.PLANT_WIN, "Every spider plant baby I put in water rooted within two weeks.", -1),
        new SamplePost(PostCategory.QUESTION, "Is a ZZ plant safe to keep in a bedroom with a curious cat?", -1),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ("open_forgot".equals(getIntent().getStringExtra("mode"))) {
            startActivity(new Intent(this,
                    ForgotPasswordActivity.class));
            finish();
            return;
        }
        log = new TextView(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        log.setPadding(pad, pad * 3, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(log);
        setContentView(scroll);

        String uid = new FirebaseAuthDataSource().getCurrentUserId();
        if (uid == null) {
            append("Sign in first, then run the seeder again.");
            return;
        }
        SharedPreferences prefs = getSharedPreferences("plantmate_debug_seed", MODE_PRIVATE);
        String mode = getIntent().getStringExtra("mode");
        if ("photo_posts".equals(mode)) { seedPhotoPosts(prefs, uid); return; }
        if ("garden".equals(mode)) { seedGarden(prefs, uid); return; }
        if ("upload_post".equals(mode)) { uploadPost(); return; }
        if ("exchange_ask".equals(mode)) { exchangeAsk(uid); return; }
        if ("exchange_rehome".equals(mode)) { exchangeRehome(uid); return; }
        if ("exchange_review".equals(mode)) { exchangeReview(uid); return; }
        if ("persona".equals(mode)) { seedPersona(prefs, uid, getIntent().getStringExtra("persona")); return; }
        String key = "seeded_" + uid;
        if (prefs.getBoolean(key, false) && !getIntent().getBooleanExtra("force", false)) {
            append("Sample data was already published for this account. Nothing to do.");
            return;
        }
        append("Publishing " + LISTINGS.length + " listings...");
        List<String> ids = new ArrayList<>(Collections.nCopies(LISTINGS.length, (String) null));
        Task<Void> chain = Tasks.forResult(null);
        for (int i = 0; i < LISTINGS.length; i++) {
            final int index = i;
            chain = chain.continueWithTask(ignored -> publish(LISTINGS[index])
                    .continueWith(task -> {
                        if (task.isSuccessful()) {
                            ids.set(index, task.getResult());
                            append("OK listing: " + LISTINGS[index].nickname);
                        } else {
                            append("FAILED listing " + LISTINGS[index].nickname + ": " + task.getException());
                        }
                        return null;
                    }));
        }
        for (SamplePost post : POSTS) {
            chain = chain.continueWithTask(ignored -> {
                String listingId = post.sharedListingIndex >= 0 ? ids.get(post.sharedListingIndex) : null;
                Task<String> create = listingId == null
                        ? community.createPost(post.text, post.category)
                        : community.createPost(post.text, post.category, listingId);
                return create.continueWith(task -> {
                    append((task.isSuccessful() ? "OK post: " : "FAILED post: ") + post.category
                            + (task.isSuccessful() ? "" : " " + task.getException()));
                    return null;
                });
            });
        }
        chain.addOnCompleteListener(ignored -> {
            prefs.edit().putBoolean(key, true).apply();
            append("Done.");
        });
    }

    private boolean alreadyDone(SharedPreferences prefs, String key) {
        if (prefs.getBoolean(key, false) && !getIntent().getBooleanExtra("force", false)) {
            append("This sample set was already added for this account. Nothing to do.");
            return true;
        }
        return false;
    }

    private void seedPhotoPosts(SharedPreferences prefs, String uid) {
        String key = "photo_posts_" + uid;
        if (alreadyDone(prefs, key)) return;
        append("Publishing " + PHOTO_POSTS.length + " community posts with photos...");
        Task<Void> chain = Tasks.forResult(null);
        for (PhotoPost post : PHOTO_POSTS) {
            chain = chain.continueWithTask(ignored -> community
                    .createPost(post.text, post.category, "", post.image)
                    .continueWith(task -> {
                        append((task.isSuccessful() ? "OK photo post: " : "FAILED photo post: ")
                                + post.category + (task.isSuccessful() ? "" : " " + task.getException()));
                        return null;
                    }));
        }
        chain.addOnCompleteListener(ignored -> {
            prefs.edit().putBoolean(key, true).apply();
            append("Done.");
        });
    }

    /**
     * Sample member flow: profile → listings → posts (sharing, photo, text) → comments and likes
     * on other members' newest posts → local Garden plants. Everything goes through the normal
     * repositories, so validation and Firestore rules apply as for a real member.
     */
    private void seedPersona(SharedPreferences prefs, String uid, String personaKey) {
        SamplePersonas.Persona persona = SamplePersonas.find(personaKey == null ? "" : personaKey);
        if (persona == null) {
            append("Unknown persona. Use --es persona minhanh, giahuy or thaovy.");
            return;
        }
        String key = "persona_" + persona.key + "_" + uid;
        if (alreadyDone(prefs, key)) return;
        append("Seeding " + persona.displayName + "...");
        Task<Void> chain = new UserRepository()
                .updateCurrentUserProfile(persona.displayName, persona.location, persona.bio)
                .continueWith(task -> {
                    append((task.isSuccessful() ? "OK profile: " : "FAILED profile: ") + persona.displayName
                            + (task.isSuccessful() ? "" : " " + task.getException()));
                    return null;
                });
        List<String> ids = new ArrayList<>(Collections.nCopies(persona.listings.length, (String) null));
        for (int i = 0; i < persona.listings.length; i++) {
            final int index = i;
            SamplePersonas.Listing l = persona.listings[i];
            chain = chain.continueWithTask(ignored -> listings.publishListing(l.nickname, l.species,
                            l.description, l.type, l.price, l.lookingFor,
                            Collections.singletonList(l.photo), persona.location)
                    .continueWith(task -> {
                        if (task.isSuccessful()) ids.set(index, task.getResult());
                        append((task.isSuccessful() ? "OK listing: " : "FAILED listing: ") + l.nickname
                                + (task.isSuccessful() ? "" : " " + task.getException()));
                        return null;
                    }));
        }
        for (SamplePersonas.Post post : persona.posts) {
            chain = chain.continueWithTask(ignored -> {
                String listingId = post.sharedListing >= 0 ? ids.get(post.sharedListing) : null;
                Task<String> create = listingId != null
                        ? community.createPost(post.text, post.category, listingId)
                        : community.createPost(post.text, post.category, "", post.photo);
                return create.continueWith(task -> {
                    append((task.isSuccessful() ? "OK post: " : "FAILED post: ") + post.category
                            + (task.isSuccessful() ? "" : " " + task.getException()));
                    return null;
                });
            });
        }
        chain = chain.continueWithTask(ignored -> community.getPostsPage(null, null)).continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                append("Could not load posts for comments: " + task.getException());
                return Tasks.forResult(null);
            }
            List<Post> others = new ArrayList<>();
            for (Post post : task.getResult().posts) {
                if (!uid.equals(post.getAuthorId())) others.add(post);
            }
            Collections.sort(others, (a, b) -> Long.compare(millis(b.getCreatedAt()), millis(a.getCreatedAt())));
            Task<Void> engage = Tasks.forResult(null);
            for (int i = 0; i < others.size() && i < 5; i++) {
                Post target = others.get(i);
                final String comment = i < persona.comments.length ? persona.comments[i] : null;
                engage = engage.continueWithTask(ignored -> community.likePost(target.getPostId())
                        .continueWithTask(liked -> comment == null ? Tasks.forResult((String) null)
                                : community.createComment(target.getPostId(), comment))
                        .continueWith(done -> {
                            append((done.isSuccessful() ? "OK like" + (comment == null ? "" : " + comment")
                                    : "FAILED engage: " + done.getException()));
                            return null;
                        }));
            }
            return engage;
        });
        chain.addOnCompleteListener(ignored -> {
            GardenPlant[] plants = new GardenPlant[persona.garden.length];
            for (int i = 0; i < plants.length; i++) {
                SamplePersonas.Plant plant = persona.garden[i];
                plants[i] = new GardenPlant(plant.nickname, plant.species, plant.photo, plant.interval,
                        i == 0 ? CareType.WATER : null, null);
            }
            addGardenPlants(plants, () -> {
                prefs.edit().putBoolean(key, true).apply();
                append("Done: " + persona.displayName);
            });
        });
    }

    /** Sample listings (by nickname) other members ask for in the exchange demo, with the message. */
    private static final String[][] EXCHANGE_ASKS = {
        {"Bụi lan ý tách chậu", "Chào bạn, mình ở gần Quận 3, cho mình xin bụi lan ý nhé!"},
        {"Cây con dây nhện", "Mình muốn xin một cây con dây nhện để treo ban công ạ."},
        {"Kim tiền đẻ nhánh", "Mình có một chậu phú quý nhỏ, đổi lấy kim tiền được không bạn?"},
        {"Trúc phất lộc thuỷ sinh", "Cho mình xin bó trúc phất lộc nha, cảm ơn bạn nhiều!"},
        {"Lá sen đá đã ra rễ", "Mình mới tập trồng sen đá, xin bạn vài lá nhé."},
        {"Ngũ gia bì", "Mình có ban công rộng, xin bạn cây ngũ gia bì nhé."},
    };

    private static final String[] OWNER_REVIEWS = {
        "Đến đúng giờ, rất lịch sự. Cây sẽ được chăm tốt!",
        "Nhận cây nhanh gọn, trao đổi dễ chịu.",
    };
    private static final String[] REQUESTER_REVIEWS = {
        "Cây khoẻ, đúng như mô tả. Chủ cây nhiệt tình chỉ cách chăm.",
        "Giao cây đúng hẹn, còn tặng thêm ít đất trồng. Rất đáng tin!",
    };

    /** Step 1 (as a requester): open a conversation on other members' demo listings and ask. */
    private void exchangeAsk(String uid) {
        ChatRepository chat =
                new ChatRepository();
        listings.getActiveListings().continueWithTask(task -> {
            Task<Void> chain = Tasks.forResult(null);
            if (!task.isSuccessful()) { append("FAILED listings: " + task.getException()); return chain; }
            int asked = 0;
            for (String[] ask : EXCHANGE_ASKS) {
                for (Listing l : task.getResult()) {
                    if (!ask[0].equals(l.getNickname()) || uid.equals(l.getOwnerId()) || asked >= 2) continue;
                    asked++;
                    chain = chain.continueWithTask(ignored -> chat.openOrCreateConversation(l.getListingId())
                            .continueWithTask(opened -> {
                                if (!opened.isSuccessful()) throw opened.getException();
                                return chat.sendTextMessage(opened.getResult(), ask[1]);
                            })
                            .continueWith(sent -> {
                                append((sent.isSuccessful() ? "OK asked: " : "FAILED ask: ") + l.getNickname()
                                        + (sent.isSuccessful() ? "" : " " + sent.getException()));
                                return null;
                            }));
                }
            }
            return chain;
        }).addOnCompleteListener(done -> append("Done: exchange_ask"));
    }

    /** Step 2 (as an owner): mark demo listings someone asked for as REHOMED, then review the requester. */
    private void exchangeRehome(String uid) {
        ChatRepository chat =
                new ChatRepository();
        ReviewRepository reviews =
                new ReviewRepository();
        chat.getConversationsForCurrentUser().continueWithTask(task -> {
            Task<Void> chain = Tasks.forResult(null);
            if (!task.isSuccessful()) { append("FAILED conversations: " + task.getException()); return chain; }
            int index = 0;
            for (Conversation c : task.getResult()) {
                if (!uid.equals(c.getOwnerId())) continue;
                final String review = OWNER_REVIEWS[index++ % OWNER_REVIEWS.length];
                chain = chain.continueWithTask(ignored -> listings.getListing(c.getListingId()))
                        .continueWithTask(l -> {
                            if (!l.isSuccessful()) throw l.getException();
                            ListingStatus status = l.getResult().getStatus();
                            if (status == ListingStatus.REHOMED) return Tasks.forResult((Void) null);
                            if (status == ListingStatus.ACTIVE) {
                                return listings.updateListingStatus(c.getListingId(),
                                        ListingStatus.RESERVED)
                                        .continueWithTask(r -> listings.updateListingStatus(c.getListingId(),
                                                ListingStatus.REHOMED));
                            }
                            return listings.updateListingStatus(c.getListingId(),
                                    ListingStatus.REHOMED);
                        })
                        .continueWithTask(rehomed -> {
                            if (!rehomed.isSuccessful()) throw rehomed.getException();
                            append("OK rehomed: " + c.getListingId());
                            return reviews.submitReview(c.getListingId(), c.getRequesterId(), 5, review);
                        })
                        .continueWith(done -> {
                            append(done.isSuccessful() ? "OK review requester" : "FAILED: " + done.getException());
                            return null;
                        });
            }
            return chain;
        }).addOnCompleteListener(done -> append("Done: exchange_rehome"));
    }

    /** Step 3 (as a requester): review the owners of REHOMED listings this member asked for. */
    private void exchangeReview(String uid) {
        ChatRepository chat =
                new ChatRepository();
        ReviewRepository reviews =
                new ReviewRepository();
        chat.getConversationsForCurrentUser().continueWithTask(task -> {
            Task<Void> chain = Tasks.forResult(null);
            if (!task.isSuccessful()) { append("FAILED conversations: " + task.getException()); return chain; }
            int index = 0;
            for (Conversation c : task.getResult()) {
                if (!uid.equals(c.getRequesterId())) continue;
                final String review = REQUESTER_REVIEWS[index % REQUESTER_REVIEWS.length];
                final int rating = index++ % 3 == 2 ? 4 : 5;
                chain = chain.continueWithTask(ignored -> reviews.submitReview(c.getListingId(), c.getOwnerId(), rating, review)
                        .continueWith(done -> {
                            append(done.isSuccessful() ? "OK review owner " + rating + "★" : "SKIPPED: " + done.getException());
                            return null;
                        }));
            }
            return chain;
        }).addOnCompleteListener(done -> append("Done: exchange_review"));
    }

    private void uploadPost() {
        String uri = null;
        try (Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?",
                new String[]{"plantmate_k%"}, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (c != null && c.moveToFirst()) {
                uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0)).toString();
            }
        }
        if (uri == null) { append("No PlantMate photo found in Pictures."); return; }
        append("Uploading " + uri + " ...");
        new ImageRepository(this).upload(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    append("OK image: " + task.getResult());
                    return community.createPost("Kệ sen đá mới xếp lại theo kiểu tổ ong, nhìn gọn mà xinh ghê. "
                            + "Ai cần lá giống thì ghé Quận 7 lấy nhé!", PostCategory.PLANT_WIN, "", task.getResult());
                })
                .addOnCompleteListener(task -> append(task.isSuccessful() ? "OK post: " + task.getResult()
                        : "FAILED: " + task.getException()));
    }

    private static long millis(Timestamp t) {
        return t == null ? 0 : t.toDate().getTime();
    }

    /** Saves each photo into the shared Pictures/PlantMate album (app-owned), then adds the plant locally. */
    private void seedGarden(SharedPreferences prefs, String uid) {
        String key = "garden_" + uid;
        if (alreadyDone(prefs, key)) return;
        addGardenPlants(GARDEN, () -> {
            prefs.edit().putBoolean(key, true).apply();
            append("Done.");
        });
    }

    private void addGardenPlants(GardenPlant[] plants, Runnable onDone) {
        GardenRepository garden = new GardenRepository(getApplicationContext());
        ExecutorService io = Executors.newSingleThreadExecutor();
        append("Adding " + plants.length + " Garden plants with photos...");
        io.execute(() -> {
            for (int index = 0; index < plants.length; index++) {
                GardenPlant plant = plants[index];
                // Sample areas and light so the Garden areas have something to group.
                String room = SAMPLE_ROOMS[index % SAMPLE_ROOMS.length];
                String light = SAMPLE_LIGHTS[index % SAMPLE_LIGHTS.length].name();
                String uri = "";
                try {
                    uri = savePhoto(plant.image, plant.nickname);
                } catch (Exception error) {
                    append("Photo failed for " + plant.nickname + ": " + error);
                }
                final String imageUri = uri;
                CountDownLatch done = new CountDownLatch(1);
                runOnUiThread(() -> garden.addPlant(plant.nickname, plant.species, imageUri, true,
                        plant.interval, "", room, light, new GardenRepository.Callback<Long>() {
                            @Override public void onSuccess(Long plantId) {
                                append("OK plant: " + plant.nickname + (imageUri.isEmpty() ? " (no photo)" : ""));
                                if (plant.care == null) { done.countDown(); return; }
                                garden.recordCare(plantId, plant.care, plant.note,
                                        new GardenRepository.Callback<CareLogEntity>() {
                                            @Override public void onSuccess(CareLogEntity value) {
                                                append("   care log: " + plant.care);
                                                done.countDown();
                                            }

                                            @Override public void onError(Exception error) {
                                                append("   care log failed: " + error);
                                                done.countDown();
                                            }
                                        });
                            }

                            @Override public void onError(Exception error) {
                                append("FAILED plant " + plant.nickname + ": " + error);
                                done.countDown();
                            }
                        }));
                try {
                    done.await();
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
            onDone.run();
        });
    }

    private String savePhoto(String url, String name) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "plantmate_" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_") + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PlantMate");
        Uri target = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (target == null) throw new IllegalStateException("MediaStore insert failed");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "PlantMate-Android/1.0");
        try (InputStream in = connection.getInputStream();
             OutputStream out = getContentResolver().openOutputStream(target)) {
            if (out == null) throw new IllegalStateException("Cannot write photo");
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        } finally {
            connection.disconnect();
        }
        return target.toString();
    }

    private Task<String> publish(SampleListing s) {
        List<String> images = s.image == null ? Collections.emptyList() : Collections.singletonList(s.image);
        return listings.publishListing(s.nickname, s.species, s.description, s.type, s.price,
                s.lookingFor, images, s.location);
    }

    private void append(String line) {
        Log.i(TAG, line);
        runOnUiThread(() -> log.append(line + "\n"));
    }
}
