package com.example.ungdungchiasecaycanh.ui.ai;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Mầm's offline care guidance. Every answer is fixed, general houseplant advice selected by
 * keyword or symptom; nothing is generated, diagnosed or sent anywhere. The UI always labels it
 * "Basic care guidance, not a diagnosis". Answers follow the app language (English or Vietnamese).
 */
public final class MamAdvisor {
    private MamAdvisor() { }

    public enum Symptom { YELLOW_LEAVES, BROWN_TIPS, DROOPING, LEAF_SPOTS, PESTS, SLOW_GROWTH }

    /** Where an answer can send the user next inside PlantMate. */
    public enum Action { NONE, OPEN_DOCTOR, OPEN_GARDEN }

    /** One guidance card: headline, ordered steps, when to look closer, and an optional next step. */
    public static final class Guidance {
        public final String title;
        public final List<String> steps;
        public final String watchFor;
        public final Action action;

        Guidance(String title, String watchFor, Action action, String... steps) {
            this.title = title;
            this.watchFor = watchFor;
            this.action = action;
            this.steps = Collections.unmodifiableList(Arrays.asList(steps));
        }
    }

    /** True when the app currently shows Vietnamese. */
    static boolean vietnamese() {
        return "vi".equals(Locale.getDefault().getLanguage());
    }

    private static String t(String en, String vi) {
        return vietnamese() ? vi : en;
    }

    /** Suggested questions shown as chips; each maps to a fixed answer below. */
    public static final String[] SUGGESTIONS = {
        "Yellow leaves on my pothos", "Best spot for a snake plant?", "How often to water an orchid?",
    };

    static final String[] SUGGESTIONS_VI = {
        "Lá trầu bà bị vàng", "Lưỡi hổ cần bao nhiêu ánh sáng?", "Bao lâu tưới lan hồ điệp một lần?",
    };

    /** Suggestion chips in the app language. */
    public static String[] suggestions() {
        return vietnamese() ? SUGGESTIONS_VI : SUGGESTIONS;
    }

    public static Guidance forSymptom(Symptom symptom) {
        switch (symptom) {
            case YELLOW_LEAVES:
                return new Guidance(t("Yellow leaves are most often a watering signal.",
                                "Lá vàng thường là dấu hiệu về cách tưới nước."),
                        t("Look closer if new leaves also turn yellow or the stem feels soft.",
                                "Hãy xem kỹ hơn nếu lá non cũng vàng hoặc thân cây bị mềm."), Action.OPEN_GARDEN,
                        t("Check the soil 2 cm down. If it is wet, let it dry before watering again.",
                                "Kiểm tra đất sâu 2 cm. Nếu còn ướt, để đất khô rồi mới tưới lại."),
                        t("Make sure the pot drains and no water sits in the saucer.",
                                "Đảm bảo chậu thoát nước tốt và không để nước đọng trong đĩa lót."),
                        t("A few old lower leaves yellowing slowly is normal; remove them.",
                                "Vài lá già phía dưới vàng dần là bình thường; hãy ngắt bỏ chúng."));
            case BROWN_TIPS:
                return new Guidance(t("Brown, crispy tips usually mean dry air or uneven watering.",
                                "Chóp lá nâu, giòn thường do không khí khô hoặc tưới không đều."),
                        t("Brown tips will not turn green again; trim them with clean scissors.",
                                "Chóp lá đã nâu sẽ không xanh lại; hãy cắt bằng kéo sạch."), Action.OPEN_GARDEN,
                        t("Water thoroughly when the top soil is dry, then empty the saucer.",
                                "Tưới đẫm khi lớp đất mặt đã khô, rồi đổ nước trong đĩa lót."),
                        t("Move the plant away from air conditioning or hot, direct sun.",
                                "Để cây xa máy lạnh và tránh nắng gắt chiếu trực tiếp."),
                        t("Flush the soil with plenty of water now and then to wash out fertiliser salts.",
                                "Thỉnh thoảng xả nhiều nước qua đất để rửa trôi muối phân bón."));
            case DROOPING:
                return new Guidance(t("Drooping comes from both too little and too much water.",
                                "Cây rũ có thể do thiếu nước hoặc thừa nước."),
                        t("If it stays limp after watering and the soil is soggy, check the roots for rot.",
                                "Nếu tưới xong cây vẫn rũ và đất sũng nước, hãy kiểm tra rễ có bị thối không."), Action.OPEN_GARDEN,
                        t("Feel the soil: bone dry means water deeply now.",
                                "Sờ đất: nếu khô hẳn thì tưới đẫm ngay."),
                        t("Soggy soil means stop watering and give it more light and airflow.",
                                "Đất sũng nước thì ngừng tưới, cho cây thêm sáng và thoáng khí."),
                        t("Some plants droop in the afternoon heat and recover at night; that is normal.",
                                "Một số cây rũ khi trưa nắng nóng và tươi lại vào ban đêm; điều đó bình thường."));
            case LEAF_SPOTS:
                return new Guidance(t("Leaf spots can come from water on leaves, sunburn or fungus.",
                                "Đốm lá có thể do nước đọng trên lá, cháy nắng hoặc nấm."),
                        t("Isolate the plant if spots spread quickly or have yellow rings.",
                                "Cách ly cây nếu đốm lan nhanh hoặc có viền vàng."), Action.NONE,
                        t("Remove the worst leaves with clean scissors.",
                                "Cắt bỏ những lá bị nặng nhất bằng kéo sạch."),
                        t("Water the soil, not the leaves, and improve airflow.",
                                "Tưới vào gốc, không tưới lên lá, và để cây thoáng khí hơn."),
                        t("Pale, dry patches after moving into sun are sunburn; shade it a little.",
                                "Mảng lá nhạt, khô sau khi chuyển ra nắng là cháy nắng; hãy che bớt nắng."));
            case PESTS:
                return new Guidance(t("Tiny webs, sticky leaves or white fluff point to pests.",
                                "Mạng tơ nhỏ, lá dính hoặc lớp bông trắng là dấu hiệu có sâu bệnh."),
                        t("Keep it away from other plants until no new pests appear for two weeks.",
                                "Để cây riêng cho đến khi hai tuần liền không thấy sâu bệnh mới."), Action.NONE,
                        t("Rinse the leaves, including the undersides, with a gentle shower.",
                                "Xịt rửa nhẹ cả mặt trên và mặt dưới lá."),
                        t("Wipe visible pests off with a cloth dipped in mild soapy water.",
                                "Lau sạch sâu rệp bằng khăn nhúng nước xà phòng loãng."),
                        t("Repeat every few days; one clean-up is rarely enough.",
                                "Lặp lại vài ngày một lần; làm một lần hiếm khi đủ."));
            case SLOW_GROWTH:
            default:
                return new Guidance(t("Slow growth is usually about light or season.",
                                "Cây chậm lớn thường do ánh sáng hoặc theo mùa."),
                        t("Only repot when roots circle the pot or grow out of the bottom.",
                                "Chỉ thay chậu khi rễ quấn quanh chậu hoặc mọc ra khỏi đáy."), Action.NONE,
                        t("Move it closer to a bright window, out of harsh midday sun.",
                                "Đưa cây lại gần cửa sổ sáng, tránh nắng gắt buổi trưa."),
                        t("Feed lightly during the growing season.",
                                "Bón phân nhẹ trong mùa sinh trưởng."),
                        t("Many plants pause in the cooler months; that is normal.",
                                "Nhiều cây tạm ngừng lớn vào mùa mát; điều đó bình thường."));
        }
    }

    public static String symptomLabel(Symptom symptom) {
        switch (symptom) {
            case YELLOW_LEAVES: return t("Yellow leaves", "Lá vàng");
            case BROWN_TIPS: return t("Brown tips", "Chóp lá nâu");
            case DROOPING: return t("Drooping", "Cây rũ");
            case LEAF_SPOTS: return t("Leaf spots", "Đốm lá");
            case PESTS: return t("Pests", "Sâu bệnh");
            default: return t("Slow growth", "Chậm lớn");
        }
    }

    static final Guidance FALLBACK = new Guidance("Here is where I can help.", "", Action.OPEN_DOCTOR,
            "Ask about watering, light, repotting, feeding or propagation.",
            "Describe a symptom: yellow or brown leaves, drooping, spots or pests.",
            "For a guided check, open Plant Doctor.");

    static final Guidance FALLBACK_VI = new Guidance("Đây là những điều Mầm có thể giúp.", "", Action.OPEN_DOCTOR,
            "Hỏi về tưới nước, ánh sáng, thay chậu, bón phân hoặc nhân giống.",
            "Mô tả triệu chứng: lá vàng hoặc nâu, cây rũ, đốm lá hay sâu bệnh.",
            "Để được kiểm tra có hướng dẫn, hãy mở Bác sĩ cây.");

    /** True for the "here is where I can help" card (no specific answer). */
    public static boolean isFallback(Guidance guidance) {
        return guidance == FALLBACK || guidance == FALLBACK_VI;
    }

    /** The honest "I can't answer that" card, in the app language. */
    static Guidance fallback() {
        return vietnamese() ? FALLBACK_VI : FALLBACK;
    }

    /** Picks a fixed answer by keyword (English and Vietnamese, accents ignored). */
    public static Guidance reply(String question) {
        String q = normalize(question);
        if (q.isEmpty()) return fallback();
        if (has(q, "yellow", "vang")) return forSymptom(Symptom.YELLOW_LEAVES);
        if (has(q, "brown", "crispy", "nau", "chay la")) return forSymptom(Symptom.BROWN_TIPS);
        if (has(q, "droop", "wilt", "limp", "heo", "ru")) return forSymptom(Symptom.DROOPING);
        if (has(q, "spots", "spotted", "dom", "nam")) return forSymptom(Symptom.LEAF_SPOTS);
        if (has(q, "pest", "bug", "gnat", "mite", "aphid", "sau benh", "bi sau", "co sau", "sau an", "rep", "con trung")) return forSymptom(Symptom.PESTS);
        if (has(q, "slow", "not growing", "cham lon")) return forSymptom(Symptom.SLOW_GROWTH);
        if (has(q, "orchid", "lan ho diep", "phong lan")) {
            return new Guidance(t("Orchids like a soak, then a full dry-out.",
                            "Lan thích được ngâm nước, rồi để khô hẳn."), "", Action.OPEN_GARDEN,
                    t("Water about once a week, when the bark feels dry and the roots look silvery.",
                            "Tưới khoảng mỗi tuần một lần, khi giá thể khô và rễ có màu bạc."),
                    t("Soak, then let it drain fully. Never leave it standing in water.",
                            "Ngâm nước rồi để ráo hẳn. Đừng bao giờ để chậu đứng trong nước."),
                    t("Keep it in bright, indirect light.",
                            "Đặt lan ở nơi sáng, tránh nắng chiếu trực tiếp."));
        }
        if (has(q, "repot", "pot", "soil", "chau", "dat")) {
            return new Guidance(t("Repot only when the roots run out of room.",
                            "Chỉ thay chậu khi rễ đã hết chỗ."), "", Action.NONE,
                    t("Look for roots circling the pot or poking out of the drainage holes.",
                            "Để ý rễ quấn quanh chậu hoặc thò ra khỏi lỗ thoát nước."),
                    t("Go up only 2 to 4 cm in pot size and use a pot with drainage.",
                            "Chỉ tăng cỡ chậu 2 đến 4 cm và dùng chậu có lỗ thoát nước."),
                    t("Use fresh, airy potting mix and water well afterwards.",
                            "Dùng đất trồng mới, tơi xốp và tưới đẫm sau khi thay."));
        }
        if (has(q, "fertili", "feed", "phan")) {
            return new Guidance(t("Feed little and only while it grows.",
                            "Bón ít và chỉ bón khi cây đang lớn."), "", Action.NONE,
                    t("Feed about once a month in the growing season.",
                            "Bón khoảng mỗi tháng một lần trong mùa sinh trưởng."),
                    t("Use half the strength on the label.",
                            "Pha loãng một nửa so với hướng dẫn trên bao bì."),
                    t("Skip feeding when the plant is resting or just repotted.",
                            "Không bón khi cây đang nghỉ hoặc vừa thay chậu."));
        }
        if (has(q, "propagat", "cutting", "giam", "nhan giong")) {
            return new Guidance(t("Cuttings root best in bright, indirect light.",
                            "Cành giâm ra rễ tốt nhất ở nơi sáng, không nắng trực tiếp."), "", Action.NONE,
                    t("Cut just below a node with clean scissors.",
                            "Cắt ngay dưới một đốt bằng kéo sạch."),
                    t("Remove the lowest leaves and root the cutting in water or damp soil.",
                            "Tỉa bớt lá dưới cùng rồi giâm cành trong nước hoặc đất ẩm."),
                    t("Pothos, spider plants and mint root especially easily.",
                            "Trầu bà, cỏ lan chi và bạc hà đặc biệt dễ ra rễ."));
        }
        if (has(q, "light", "sun", "shade", "spot", "anh sang", "nang", "cho dat")) {
            return new Guidance(t("Bright, indirect light suits most houseplants.",
                            "Ánh sáng tán xạ phù hợp với hầu hết cây trong nhà."), "", Action.NONE,
                    t("A spot 1 to 2 m from a window works well.",
                            "Vị trí cách cửa sổ 1 đến 2 m là lý tưởng."),
                    t("Succulents and cacti want direct sun; snake plants and ZZ plants cope with low light.",
                            "Sen đá và xương rồng cần nắng trực tiếp; lưỡi hổ và kim tiền chịu được thiếu sáng."),
                    t("Keep plants away from cold drafts and air conditioning.",
                            "Để cây tránh gió lạnh và máy lạnh."));
        }
        if (has(q, "water", "tuoi", "nuoc")) {
            return new Guidance(t("Water by feel, not by calendar.",
                            "Tưới theo cảm nhận đất, không theo lịch."), "", Action.OPEN_GARDEN,
                    t("Water when the top 2 cm of soil is dry.",
                            "Tưới khi lớp đất 2 cm trên cùng đã khô."),
                    t("Water until it drains, then empty the saucer.",
                            "Tưới đến khi nước chảy ra đáy, rồi đổ nước trong đĩa lót."),
                    t("Succulents and snake plants need less; ferns and peace lilies like steadier moisture.",
                            "Sen đá và lưỡi hổ cần ít nước; dương xỉ và lan ý thích ẩm đều hơn."));
        }
        if (has(q, "aloe", "succulent", "cactus", "jade", "crassula", "haworthia", "sen da", "xuong rong", "nha dam")) {
            return new Guidance(t("Succulents want sun and a full dry-out.",
                            "Sen đá cần nắng và đất khô hẳn giữa các lần tưới."), "", Action.NONE,
                    t("Give it the sunniest window you have.",
                            "Đặt cây ở ô cửa nắng nhất nhà."),
                    t("Water deeply, then wait until the soil is completely dry.",
                            "Tưới đẫm, rồi đợi đến khi đất khô hoàn toàn."),
                    t("Use gritty, fast-draining soil and a pot with drainage.",
                            "Dùng đất nhiều sỏi, thoát nước nhanh và chậu có lỗ thoát nước."));
        }
        if (has(q, "peace lily", "spathiphyllum", "lan y")) {
            return new Guidance(t("Peace lilies tell you when they are thirsty.",
                            "Lan ý sẽ cho bạn biết khi nào nó khát."), "", Action.OPEN_GARDEN,
                    t("Water when the top soil is dry or the leaves just start to droop.",
                            "Tưới khi đất mặt khô hoặc lá vừa bắt đầu rũ."),
                    t("Bright, indirect light keeps it flowering; avoid direct afternoon sun.",
                            "Ánh sáng tán xạ giúp cây ra hoa; tránh nắng chiều trực tiếp."),
                    t("Wipe the broad leaves now and then so they stay glossy.",
                            "Thỉnh thoảng lau lá to để lá luôn bóng."));
        }
        if (has(q, "pothos", "epipremnum", "trau ba")) {
            return new Guidance(t("Pothos is one of the easiest plants to share.",
                            "Trầu bà là một trong những cây dễ chia sẻ nhất."), "", Action.NONE,
                    t("Let the top few centimetres dry between waterings.",
                            "Để vài cm đất mặt khô giữa hai lần tưới."),
                    t("It grows in low light but stays fuller in bright, indirect light.",
                            "Cây sống được khi thiếu sáng nhưng sẽ rậm hơn ở nơi sáng tán xạ."),
                    t("Cuttings root in water in a week or two.",
                            "Cành giâm trong nước ra rễ sau một đến hai tuần."));
        }
        if (has(q, "zz", "zamioculcas", "kim tien")) {
            return new Guidance(t("ZZ plants store water in their roots.",
                            "Cây kim tiền trữ nước trong củ rễ."), "", Action.NONE,
                    t("Water only when the soil is completely dry, often every 2 to 3 weeks.",
                            "Chỉ tưới khi đất khô hẳn, thường 2 đến 3 tuần một lần."),
                    t("Low to bright, indirect light both work.",
                            "Nơi ít sáng hay sáng tán xạ đều được."),
                    t("Yellowing stems usually mean too much water.",
                            "Thân vàng thường là do tưới quá nhiều."));
        }
        if (has(q, "spider plant", "chlorophytum", "co lan chi")) {
            return new Guidance(t("Spider plants love to make babies.",
                            "Cỏ lan chi rất hay ra cây con."), "", Action.NONE,
                    t("Keep the soil lightly moist and give it bright, indirect light.",
                            "Giữ đất hơi ẩm và cho cây ánh sáng tán xạ."),
                    t("Brown tips often come from hard tap water; rainwater helps.",
                            "Chóp lá nâu thường do nước máy cứng; dùng nước mưa sẽ đỡ hơn."),
                    t("Pot the baby plantlets once they grow small roots.",
                            "Trồng cây con ra chậu khi chúng đã có rễ nhỏ."));
        }
        if (has(q, "fiddle", "ficus", "bang singapore")) {
            return new Guidance(t("Fiddle leaf figs like a steady routine.",
                            "Bàng Singapore thích một thói quen ổn định."), "", Action.NONE,
                    t("Give it a bright spot and avoid moving it around.",
                            "Đặt cây ở nơi sáng và tránh di chuyển nhiều."),
                    t("Water when the top 5 cm of soil is dry.",
                            "Tưới khi lớp đất 5 cm trên cùng đã khô."),
                    t("Dropping lower leaves often means a change in light or watering.",
                            "Rụng lá dưới thường do thay đổi ánh sáng hoặc cách tưới."));
        }
        if (has(q, "mint", "mentha", "herb", "bac ha", "rau thom")) {
            return new Guidance(t("Mint grows fast and loves water.",
                            "Bạc hà lớn nhanh và rất thích nước."), "", Action.OPEN_GARDEN,
                    t("Keep the soil evenly moist and give it a few hours of sun.",
                            "Giữ đất ẩm đều và cho cây vài giờ nắng."),
                    t("Pinch the tips often to keep it bushy.",
                            "Thường xuyên bấm ngọn để cây rậm."),
                    t("Grow it in its own pot so it does not take over.",
                            "Trồng riêng một chậu để bạc hà không lấn các cây khác."));
        }
        if (has(q, "snake", "luoi ho", "sansevieria", "dracaena")) {
            return new Guidance(t("Snake plants thrive on neglect.",
                            "Lưỡi hổ càng ít chăm càng khỏe."), "", Action.NONE,
                    t("Bright, indirect light is ideal, but it copes with low light.",
                            "Ánh sáng tán xạ là lý tưởng, nhưng cây vẫn chịu được thiếu sáng."),
                    t("Water every 2 to 3 weeks, only when the soil is completely dry.",
                            "Tưới 2 đến 3 tuần một lần, chỉ khi đất đã khô hẳn."),
                    t("Keep it away from cold drafts.",
                            "Để cây tránh gió lạnh."));
        }
        return fallback();
    }

    static String normalize(String text) {
        if (text == null) return "";
        String lower = text.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static boolean has(String haystack, String... needles) {
        String padded = " " + haystack.replaceAll("[^a-z0-9 ]", " ") + " ";
        for (String n : needles) {
            if (n.length() <= 3 ? padded.contains(" " + n + " ") : padded.contains(n)) return true;
        }
        return false;
    }
}
