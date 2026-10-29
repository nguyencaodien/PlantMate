package com.example.ungdungchiasecaycanh.debug;

import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.PostCategory;

/**
 * Debug-only sample community: three members, each with a profile, seven listings, a few posts,
 * comments and Garden plants. Photos are the project's own plant photos served from Firebase
 * Hosting (hosting/public/plants, deployed with `firebase deploy --only hosting`).
 */
final class SamplePersonas {
    private SamplePersonas() { }

    static final String PHOTOS = "https://plantmate-be289.web.app/plants/";

    static final class Listing {
        final ListingType type;
        final String nickname, species, description, lookingFor, photo;
        final long price;

        Listing(ListingType type, String nickname, String species, String description,
                long price, String lookingFor, String photo) {
            this.type = type; this.nickname = nickname; this.species = species;
            this.description = description; this.price = price; this.lookingFor = lookingFor;
            this.photo = PHOTOS + photo;
        }
    }

    static final class Post {
        final PostCategory category;
        final String text, photo;
        /** Index into the persona's listings for a SHARING post, or -1. */
        final int sharedListing;

        Post(PostCategory category, String text, String photo, int sharedListing) {
            this.category = category; this.text = text;
            this.photo = photo == null ? "" : PHOTOS + photo;
            this.sharedListing = sharedListing;
        }
    }

    static final class Plant {
        final String nickname, species, photo;
        final int interval;

        Plant(String nickname, String species, String photo, int interval) {
            this.nickname = nickname; this.species = species; this.photo = PHOTOS + photo; this.interval = interval;
        }
    }

    static final class Persona {
        final String key, displayName, location, bio;
        final Listing[] listings;
        final Post[] posts;
        final String[] comments;
        final Plant[] garden;

        Persona(String key, String displayName, String location, String bio, Listing[] listings,
                Post[] posts, String[] comments, Plant[] garden) {
            this.key = key; this.displayName = displayName; this.location = location; this.bio = bio;
            this.listings = listings; this.posts = posts; this.comments = comments; this.garden = garden;
        }
    }

    static final Persona MINH_ANH = new Persona("minhanh", "Nguyễn Minh Anh", "Quận 3, TP.HCM",
            "Mê cây lá trong nhà. Hay tách cây con tặng hàng xóm quanh Quận 3.",
            new Listing[]{
                new Listing(ListingType.GIVE_AWAY, "Bụi lan ý tách chậu", "Spathiphyllum wallisii",
                        "Bụi lan ý khoẻ, vừa tách từ chậu lớn đang ra hoa trắng. Hợp để phòng khách hoặc góc ít nắng.",
                        0, "", "cay-lan-y.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Cây con dây nhện", "Chlorophytum comosum",
                        "Mấy cây con dây nhện đã có rễ, trồng chậu treo rất đẹp. Bạn mang theo túi hoặc cốc nhỏ nhé.",
                        0, "", "cay-day-nhen.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Vạn niên thanh cao 60 cm", "Dieffenbachia seguine",
                        "Cây lá to, xanh quanh năm. Mình chuyển nhà nhỏ hơn nên muốn tìm chủ mới chăm giúp.",
                        0, "", "cay-van-nien-thanh.jpg"),
                new Listing(ListingType.TRADE, "Chậu trường sinh", "Peperomia obtusifolia",
                        "Lá dày, bóng, rất dễ chăm, tưới khoảng 1 tuần một lần.",
                        0, "Một nhánh trầu bà cẩm thạch", "cay-truong-sinh.jpg"),
                new Listing(ListingType.TRADE, "Hồng môn đang ra hoa", "Anthurium andraeanum",
                        "Hồng môn hoa đỏ, đang có 3 bông. Cây để nơi sáng, tránh nắng gắt buổi chiều.",
                        0, "Sen đá hoặc xương rồng nhỏ", "cay-hong-mon.jpg"),
                new Listing(ListingType.SELL, "Ngọc ngân chậu sứ", "Aglaonema 'Snow White'",
                        "Ngọc ngân lá đốm trắng, kèm chậu sứ trắng. Hợp để bàn làm việc.",
                        150000, "", "cay-ngoc-ngan.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Mầm lưỡi hổ", "Dracaena trifasciata",
                        "Ba mầm lưỡi hổ tách từ chậu mẹ, rất hợp cho người mới trồng.",
                        0, "", "cay-luoi-ho.jpg"),
            },
            new Post[]{
                new Post(PostCategory.SHARING, "Bụi lan ý nhà mình ra hoa liên tục nên tách được mấy bụi nhỏ. Ai ở gần Quận 3 cần thì nhắn mình nhé!", null, 0),
                new Post(PostCategory.PLANT_WIN, "Chậu dây nhện treo ban công ra cả chục cây con, nhìn như thác xanh luôn.", "cay-day-nhen.jpg", -1),
                new Post(PostCategory.QUESTION, "Hồng môn nhà mình ra hoa đẹp nhưng mép lá hơi vàng. Có phải do nắng chiều chiếu thẳng không mọi người?", "cay-hong-mon.jpg", -1),
                new Post(PostCategory.TIP, "Mẹo nhỏ: lau lá vạn niên thanh bằng khăn ẩm mỗi tuần, lá bóng và hấp thụ ánh sáng tốt hơn hẳn.", null, -1),
            },
            new String[]{
                "Đẹp quá! Bạn chăm kiểu gì mà cây xanh vậy?",
                "Mình cũng từng gặp y chang, để cây chỗ sáng hơn một chút là đỡ nhiều.",
                "Hóng thêm ảnh cập nhật nha bạn.",
            },
            new Plant[]{
                new Plant("Bé Lan", "Spathiphyllum wallisii", "cay-lan-y.jpg", 4),
                new Plant("Treo ban công", "Chlorophytum comosum", "cay-day-nhen.jpg", 3),
                new Plant("Lưỡi hổ góc cửa", "Dracaena trifasciata", "cay-luoi-ho.jpg", 14),
            });

    static final Persona GIA_HUY = new Persona("giahuy", "Trần Gia Huy", "TP. Thủ Đức, TP.HCM",
            "Thích cây phong thuỷ và bonsai mini. Sẵn sàng đổi cây với anh em quanh Thủ Đức.",
            new Listing[]{
                new Listing(ListingType.TRADE, "Kim tiền đẻ nhánh", "Zamioculcas zamiifolia",
                        "Chậu kim tiền ra nhiều nhánh mới, chịu được ánh sáng yếu trong phòng.",
                        0, "Cây phú quý hoặc hồng môn", "cay-kim-tien.jpg"),
                new Listing(ListingType.SELL, "Kim ngân 3 thân", "Pachira aquatica",
                        "Kim ngân 3 thân kèm chậu gốm hồng, cao khoảng 45 cm. Hợp làm quà tân gia.",
                        220000, "", "cay-kim-ngan.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Trúc phất lộc thuỷ sinh", "Dracaena sanderiana",
                        "Một bó trúc phất lộc trồng nước, chỉ cần thay nước mỗi tuần.",
                        0, "", "cay-truc-phat-loc.jpg"),
                new Listing(ListingType.SELL, "Bonsai si Đài Loan", "Ficus microcarpa",
                        "Si Đài Loan đã uốn dáng 2 năm, rễ nổi đẹp. Giao tận tay ở Thủ Đức.",
                        350000, "", "cay-si-dai-loan.jpg"),
                new Listing(ListingType.TRADE, "Tùng bồng lai mini", "Juniperus procumbens 'Nana'",
                        "Tùng bồng lai dáng tròn, lá kim mềm, để bàn làm việc rất xinh.",
                        0, "Một chậu tuyết tùng nhỏ", "cay-tung-bong-lai.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Thiết mộc lan cao 1 m", "Dracaena fragrans",
                        "Cây cao khoảng 1 m, khoẻ. Nặng nên bạn tự đến lấy giúp mình nhé.",
                        0, "", "cay-thiet-moc-lan.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Chậu tài lộc mini", "Macadamia (cây tài lộc)",
                        "Còn vài chậu tài lộc mini sau Tết, tặng bạn nào muốn thêm chút xanh cho bàn học.",
                        0, "", "cay-tai-loc.jpg"),
            },
            new Post[]{
                new Post(PostCategory.SHARING, "Chậu kim tiền đẻ nhánh nhiều quá, mình muốn đổi lấy một cây phú quý hoặc hồng môn cho góc bàn làm việc.", null, 0),
                new Post(PostCategory.PLANT_WIN, "Sau 2 năm uốn tỉa, cây si Đài Loan của mình cuối cùng cũng ra dáng bonsai.", "cay-si-dai-loan.jpg", -1),
                new Post(PostCategory.QUESTION, "Trúc phất lộc thuỷ sinh nên thay nước bao lâu một lần? Nước nhà mình hơi đục sau một tuần.", "cay-truc-phat-loc.jpg", -1),
                new Post(PostCategory.GENERAL, "Anh em Thủ Đức có ai muốn lập nhóm đổi cây cuối tuần không? Mỗi tháng gặp một lần ở công viên cho vui.", null, -1),
                new Post(PostCategory.TIP, "Kim ngân không cần tưới nhiều đâu, mình chờ đất khô 3 đến 4 cm mới tưới, cây xanh đều cả năm.", "cay-kim-ngan.jpg", -1),
            },
            new String[]{
                "Cây đẹp ghê, mình ở Thủ Đức, có dịp đổi cây với bạn nhé.",
                "Theo mình nên để chỗ sáng tán xạ, đừng để nắng trưa chiếu thẳng.",
                "Lưu lại mẹo này, cảm ơn bạn đã chia sẻ!",
            },
            new Plant[]{
                new Plant("Si bonsai", "Ficus microcarpa", "cay-si-dai-loan.jpg", 3),
                new Plant("Kim tiền văn phòng", "Zamioculcas zamiifolia", "cay-kim-tien.jpg", 14),
                new Plant("Trúc bình thuỷ", "Dracaena sanderiana", "cay-truc-phat-loc.jpg", 7),
            });

    static final Persona THAO_VY = new Persona("thaovy", "Lê Thảo Vy", "Quận 7, TP.HCM",
            "Sen đá, cây để bàn và thảo mộc. Góc làm việc của mình lúc nào cũng xanh.",
            new Listing[]{
                new Listing(ListingType.GIVE_AWAY, "Lá sen đá đã ra rễ", "Echeveria",
                        "Rất nhiều lá sen đá đã ra rễ, hợp cho bạn mới tập trồng. Tặng miễn phí ở Quận 7.",
                        0, "", "cay-sen-da.jpg"),
                new Listing(ListingType.TRADE, "Chậu hương thảo", "Salvia rosmarinus",
                        "Hương thảo thơm, đã quen nắng ban công. Cắt ngọn nấu ăn được luôn.",
                        0, "Bạc hà hoặc húng quế", "cay-huong-thao.jpg"),
                new Listing(ListingType.SELL, "Phú quý lá hồng", "Aglaonema 'Lady Valentine'",
                        "Phú quý lá viền hồng, cây đầy chậu. Hợp để bàn hoặc kệ sách.",
                        180000, "", "cay-phu-quy.jpg"),
                new Listing(ListingType.SELL, "Cặp tuyết tùng mini", "Cupressus macrocarpa 'Goldcrest'",
                        "Hai chậu tuyết tùng nhỏ, mùi chanh nhẹ khi chạm vào lá. Bán cả cặp.",
                        160000, "", "cay-tuyet-tung.jpg"),
                new Listing(ListingType.TRADE, "Cau tiểu trâm", "Dypsis lutescens",
                        "Chậu cau cao khoảng 70 cm, lá xanh mướt, hợp phòng khách sáng.",
                        0, "Lưỡi hổ hoặc kim tiền", "cay-cau.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Ngũ gia bì", "Schefflera arboricola",
                        "Cây ngũ gia bì khoẻ, mình cần thêm chỗ cho kệ sen đá nên tặng lại.",
                        0, "", "cay-ngu-gia-bi.jpg"),
                new Listing(ListingType.GIVE_AWAY, "Trạng nguyên sau Tết", "Euphorbia pulcherrima",
                        "Trạng nguyên lá đỏ còn đẹp sau Tết, tặng bạn nào có ban công nhiều nắng.",
                        0, "", "cay-hoa-trang-nguyen.jpg"),
            },
            new Post[]{
                new Post(PostCategory.SHARING, "Mình có rất nhiều lá sen đá đã ra rễ, tặng miễn phí cho bạn nào mới tập trồng ở Quận 7.", null, 0),
                new Post(PostCategory.TIP, "Hương thảo thích nắng và gió, để trong phòng kín là cây rụng lá ngay. Ban công hướng đông là đẹp nhất.", "cay-huong-thao.jpg", -1),
                new Post(PostCategory.PLANT_WIN, "Hai chậu tuyết tùng mini cho bàn làm việc, nhìn như cây thông Noel tí hon.", "cay-tuyet-tung.jpg", -1),
                new Post(PostCategory.QUESTION, "Ngũ gia bì của mình bị rụng lá dưới gốc sau khi chuyển vào phòng máy lạnh. Có cách nào cứu không?", null, -1),
                new Post(PostCategory.GENERAL, "Góc phòng khách có thêm chậu cau tiểu trâm, không khí dễ chịu hẳn.", "cay-cau.jpg", -1),
            },
            new String[]{
                "Xinh quá trời! Mình cũng đang muốn trồng thử loài này.",
                "Bạn thử kiểm tra đất xem có úng không, mình từng bị vậy do tưới nhiều.",
                "Cho mình xin một nhánh nhé, mình ở Quận 7 nè.",
            },
            new Plant[]{
                new Plant("Kệ sen đá", "Echeveria", "cay-sen-da.jpg", 10),
                new Plant("Hương thảo ban công", "Salvia rosmarinus", "cay-huong-thao.jpg", 3),
                new Plant("Tuyết tùng bàn làm việc", "Cupressus macrocarpa 'Goldcrest'", "cay-tuyet-tung.jpg", 4),
            });

    static Persona find(String key) {
        for (Persona p : new Persona[]{MINH_ANH, GIA_HUY, THAO_VY}) if (p.key.equals(key)) return p;
        return null;
    }
}
