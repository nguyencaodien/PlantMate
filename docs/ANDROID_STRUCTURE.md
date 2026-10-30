# Android Structure

## Package tree

```text
com.example.ungdungchiasecaycanh/
├─ data/
│  ├─ local/dao/
│  ├─ local/entity/
│  ├─ local/PlantMateDatabase.java
│  ├─ remote/
│  └─ repository/
├─ model/
├─ ui/
│  ├─ auth/       ├─ main/       ├─ home/       ├─ listing/
│  ├─ garden/     ├─ doctor/     ├─ ai/         ├─ community/
│  ├─ inbox/      ├─ profile/    ├─ settings/   └─ sync/
├─ adapter/
├─ notification/
├─ receiver/
├─ scheduler/
└─ util/
```

`data/local` contains Room database, DAOs, and entities. `data/remote` contains Firebase-facing data sources. `data/repository` coordinates data access. `model` holds domain models. Each `ui` feature contains its Activities, Fragments, ViewModels, and feature-local UI helpers. `adapter` holds RecyclerView adapters. Notification, receiver, scheduler, and util packages have their stated Android responsibilities.

Listing publication follows `CreateListingActivity` →
`ListingPreviewActivity` → Listing ViewModel/Repository →
`FirebaseListingDataSource`. The Firestore Listing retains all twelve fields;
the required `imageUrls` list may be empty and is capped at five. With remote
upload unavailable, publication uses `[]` and adapters/details use the local
PlantMate placeholder. Android local image URIs are preview-only and are not
written to the remote Listing. Preview-to-publication wiring is implemented in
the dedicated publication step, without adding Firebase Storage.

The planned Garden local package structure is:

data/local/PlantMateDatabase.java
data/local/dao/PlantDao.java and CareLogDao.java
data/local/entity/PlantEntity.java and CareLogEntity.java
data/local/converter/CareTypeConverter.java when needed
data/repository/GardenRepository.java
ui/garden/GardenFragment.java, GardenViewModel.java, PlantAdapter.java,
AddPlantActivity.java, PlantDetailActivity.java, EditPlantActivity.java,
and optional AddCareLogBottomSheet.java/CareLogAdapter.java.

Room is currently version 5 (explicit 1→2 Doctor-history, 2→3 Doctor identification, 3→4 daily watering times and 4→5 Doctor feedback migrations);
destructive migrations are not the long-term strategy. Garden UI/ViewModels call GardenRepository, never
DAOs directly. Plant lists and Plant Detail are always filtered by the
authenticated ownerUid. The eleven-field PlantEntity and five-field
CareLogEntity contracts, local image URI policy, reminder invariants, and
three implementation phases are frozen in DATA_MODEL.md.

The planned Java messaging classes are `model/Conversation.java`,
`model/Message.java`, and `model/MessageType.java`;
`data/repository/ChatRepository.java` and
`data/remote/FirebaseChatDataSource.java`; and under `ui/inbox/`,
`InboxActivity.java`, `InboxViewModel.java`, `ConversationActivity.java`,
`ConversationViewModel.java`, `ConversationAdapter.java`, and
`MessageAdapter.java`. Inbox and Conversation Activities receive stable IDs
only and reload their data through ViewModels. The Conversation document ID
is `{listingId}__{requesterId}`; an existing Conversation Activity receives
`conversationId` only. These planned classes do not require a participant
subcollection.

## UI policy

- `MainActivity` hosts the five primary Fragments: Home, Garden, Doctor, Community, and Profile.
- Authentication and contextual/detail flows use Activities.
- ViewModels remain feature-local, retain UI state, and use LiveData where appropriate.
- Activities and Fragments call repositories through ViewModels; they do not perform direct Room/Firebase data access.
- Repositories are organized by data domain and do not require an interface/use-case layer for every operation.
- Inbox and Conversation UI call `ChatRepository` through feature ViewModels; Firebase access stays in `FirebaseChatDataSource`. Conversation reads and message reads are participant-scoped. Messages are TEXT, IMAGE or VOICE (see `DATA_MODEL.md`). Inbox and Conversation update in real time while the screen is started; there is no manual refresh button.
- `MainActivity` is not exported and sends a signed-out member to Sign in (clearing the task) before showing any tab.

## RecyclerView policy

Use the approved adapter names: `ListingAdapter`, `GardenAdapter`, `PostAdapter`, `CommentAdapter`, `ConversationAdapter`, `MessageAdapter`, and `CareLogAdapter`. Reuse `ListingAdapter` for My Listings or Saved Plants only if layout and behavior genuinely match.

Every RecyclerView list submits through `util/DiffList` (none calls `notifyDataSetChanged`), except the Home featured strip (`HomeHeroAdapter`, at most 8 cards shown twice so the drift can loop), which rebinds all when its cards change. Adapters with a fixed leading row (the Garden shelf's "add" tile, the story strip's "create" card, Mầm's intro) pass an offset of 1; trailing tiles (story "add friends", Mầm typing, photo-draft camera/library) are removed before and inserted after the diff. Each adapter gives a stable ID (listing, post, plant, log, conversation or message ID) and the values its row displays; DiffList records those values when a list is submitted and dispatches only inserts, removals and changed rows (rebound in place with the `DiffList.REBIND` payload, no cross-fade). Relative times and watering status are part of the displayed values, so a new day or minute still updates the row. `MessageAdapter` keeps its separate `"playback"` payload for voice progress. When one setting changes every row (the signed-in account for chat bubbles or comment actions), the adapter calls `notifyItemRangeChanged(0, size, DiffList.REBIND)`.

## Resources

```text
res/
├─ layout/     drawable/     menu/     xml/     font/     mipmap/
├─ values/     colors.xml, dimens.xml, strings.xml, styles.xml, themes.xml
├─ values-night/
└─ values-vi/  strings.xml (Vietnamese; format arguments match values/)
```

`xml/locales_config.xml` lists the app languages (en, vi); `xml/file_paths.xml` exposes only
`filesDir/doctor_photos/` to the camera app through `androidx.core.content.FileProvider`.

Layout names use `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`, and `bottom_sheet_*.xml`. Put user-visible strings, colors, and dimensions in resources when appropriate.

Java code does not hard-code ARGB colours. Text on the fixed pastel type badges and status pills uses `badge_{give,trade,sell}_text` and `status_{active,reserved,rehomed,unavailable}_ink` (plus `status_*_dot`); the heart on the white photo button uses `saved_heart` / `photo_button_ink`. These are deliberately not overridden in `values-night`, because their backgrounds stay light in both themes. The only literal colours left are the avatar preset palette and alpha masks derived from theme colours. Classes are imported rather than written with fully-qualified names inline.

### Visual tokens (canvas B2 v3.2, 2026-10-14)

The approved visual system (white ground, ink `#111412` for type, primary buttons and strong surfaces, green `#1F7A45` for the second action and state, grey panels instead of outlined cards) lives in resources, not in layouts. `DESIGN.md` is the rulebook; this section says where things are.

- `values/colors.xml` and `values-night/colors.xml` define every token for both themes. Canvas B adds `color_panel` / `color_panel_line` (grey panels and their hairlines), `color_rank_panel`, `nav_capsule`, `nav_inactive`, `nav_active_bg`, `nav_active_ink` and `rank_track`. Text on ink uses `color_background`, which inverts with the theme. `values/themes.xml` maps every Material 3 colour role to these tokens.
- `values/styles.xml`: type scale `TextAppearance.PlantMate.*` (system Roboto, headings 700, labels 500, body 400); buttons `Widget.PlantMate.Button` (ink), `.Button.Green`, `.Button.Outlined`, `.Button.OnInk` (+ `.Small`); `PlantMateFilterChip` (ink when chosen); `Widget.PlantMate.UnderlineTabs` (TabLayout); inputs with a grey fill and a 2dp ink focus ring; per-screen row and stat styles (`PlantMateProfileStat*`, `PlantMateAccountRow`, `PlantMateRankBadge`, `PlantMateOnboarding*`).
- Radii: `corner_radius_control` 14dp, panels 16–20dp (`bg_panel_16`, `bg_panel_20`, `bg_tint_panel_16`, `bg_rank_panel_20`, `bg_ink_panel_20`), tags 7dp (`bg_tag_green`, `bg_tag_ink`, `bg_tag_white`, `bg_tag_soft`, `bg_tag_grey`). Rows inside a panel are split by `bg_top_hairline_panel`.
- Logo `drawable-nodpi/logo_plantmate.png` (shown by `drawable/ic_plantmate_brand.xml` at 38dp) and the adaptive launcher icon (`mipmap-anydpi/ic_launcher*.xml`: `@color/launcher_background`, `ic_launcher_logo_foreground.png`, `ic_launcher_logo_monochrome.png`, made from the 2000 px source); Mầm (leaf-mane mascot, approved canvas v4, 2026-10-21: leaf-star mane, three crown leaves, bear ears, charcoal eyes with one highlight, wide laughing mouth) is `drawable/ic_mam.xml` (waving) / `avd_mam.xml` (hop, shadow, blink and top-leaf sway; groups `hop`, `shadow`, `blink`, `sprout`) plus poses `ic_mam_look` (glasses, magnifier), `ic_mam_happy`, `ic_mam_worry`, `ic_mam_unsure` and `ic_mam_help`.
- Motion tokens live in `util/Motion.java` and `res/animator` / `res/interpolator`; everything respects the system animation scale.
- Navigation: `MainActivity` keeps the five-item bar (Home, Garden, Doctor, Community, Profile) as `ui/NavCapsuleView`, a `BottomNavigationView` whose `CapsuleMenuView` gives the selected item its label width (≤ 46% of the capsule) and shares the rest; icons sit beside labels (`itemIconGravity` start). There is no raised scan button.
- Home: `item_home_header` (the member's area as a plain label with a green pin, not a filter; inbox and saved, the "Nổi bật quanh bạn" strip `HomeHeroAdapter` (mockup C1: 160×220dp photo cards with type tag, name and the sharer's avatar and name, up to 8, scrolled sideways; 6 s after Home opens (or after the last touch) the strip drifts right to left at 28dp/s and loops, since the cards repeat once; a touch stops it; cards press to 97%; no drift with reduced motion) with the "Đăng tin" button beside the title, search, the "Vườn hôm nay" card (mockup E2: green water disc, today's status, a done/due bar, up to three of the member's plant photos), a thin vine divider with three leaves, "Tin mới quanh bạn" with type chips), the listing grid (`item_listing`), then `item_home_sales` (urgent give-aways, active givers, cheapest sales, the post CTA).
- Listing detail: photo with thumbnail strip, tags, facts grid (`item_detail_fact_cell`), owner panel with stats, pickup panel, similar listings. Create listing: type cards, photos, steppers (`view_stepper`), category dropdown, age chips, negotiable switch.
- Garden: the week as canvas option J (the selected day as a big date with weekday and month, tap it for today; then seven `item_week_day` dots: hollow grey = no care, solid green = care planned, green ring = all done), today's tasks (`item_garden_task`, liquid-fill water button `bg_water_button`), Mầm tip (`GardenTips`), zones, plant grid (`GardenShelfAdapter`, `item_garden_plant_card`), recent activity. Plant detail: photo, white sheet, underline tabs (overview / plan / log), `CareHeatmapView`.
- Doctor: viewfinder frame, library and camera, plant link, green "Analyze", recent checks (`item_doctor_recent`), symptom chips and Mầm's guidance. Doctor result: photo, condition and confidence panel, Mầm's note, "What to do" checklist (`item_treatment_check`: green box and "Done" tag, never struck through, "N / M done").
- Mầm chat: grey Mầm bubbles, ink member bubbles, camera (opens Doctor) / input / send composer.
- Community: one title row with the inbox button and "Đăng bài" (canvas option C), the search square beside the composer, story strip (`item_story_card`, canvas option C: narrow 110×174dp story cards with the author's avatar and name, no autoplay), composer panel, underline category tabs, flat posts over hairlines (`item_post`) with the shared listing as a panel (`CommunityViewModel.loadListings`).
- Profile: member row, ink "Edit profile" and green "My garden", four numbers, the rank panel (`Reputation`), the ink "still available?" card, Listed / Rehomed / Saved tabs (`item_profile_listing`, `item_profile_row`), the newest review, the account panel.
- Chat: sent bubbles ink, received grey panel; listing panel with the Asked → Arranged → Picked up line (`item_status_step`); mic / input with photo / send composer. `ui/inbox/` holds `VoiceRecorder` (AAC, 60 s cap), `VoicePlayer` and `WaveformView`. Photo and voice data go through `ChatRepository` → `FirebaseChatMediaDataSource`.
- Onboarding: progress bar and "Skip", area chips plus the area field, intent cards (`item_onboarding_choice`), a reminder preview, back and "Continue".
- Launch: `ui/auth/SplashActivity` (launcher, `Theme.PlantMate.Launch`, `values-v31` blanks the system splash icon) draws `avd_brand_ring`, blooms the logo over `bg_brand_glow` and routes by session.
- Sign in: a deep-green brand block (`bg_brand_block`, clipped by its outline) with three columns of plant photos (`photo_signin_*`, filled in `SignInActivity`) drifting slowly, middle up and sides down, faded at both edges (`bg_brand_fade_top/bottom`) under the logo and name; still with reduced motion; then the plain form: email, password with show/hide, forgot password, sign in, Google / Facebook, register.

The planned Review classes are `model/Review.java`,
`data/remote/FirebaseReviewDataSource.java`,
`data/repository/ReviewRepository.java`, and under `ui/review/`,
`ReviewViewModel.java`, `UserReviewsActivity.java`, `ReviewAdapter.java`, and
`RateUserBottomSheet.java`. They follow
UI → ViewModel → Repository → DataSource and use stable IDs; no direct
Firebase calls belong in Review UI. Conversation remains the preferred
eligibility entry point, and reviewer profiles are resolved through
`UserRepository` rather than copied into Review documents.

The planned Community classes are `model/Post.java`, `model/PostCategory.java`,
and `model/Comment.java`; `data/remote/FirebaseCommunityDataSource.java` and
`data/repository/CommunityRepository.java`; and under `ui/community/`,
`CommunityViewModel.java`, `PostAdapter.java`, `CreatePostActivity.java`,
`PostDetailActivity.java`, `PostDetailViewModel.java`, and `CommentAdapter.java`.
CommunityFragment remains the primary tab. Stable `postId` and optional
`listingId` extras are passed to secondary Activities; full domain objects are
reloaded through ViewModels. Community UI does not call Firebase directly.

Notifications live under `ui/notifications/` (`NotificationsActivity`,
`NotificationsViewModel`, `NotificationAdapter`, and `BellViewModel` for the Home
badge), with `data/repository/NotificationRepository.java` and
`data/remote/FirebaseNotificationDataSource.java`; layouts `activity_notifications`
and `item_notification`.

Doctor remains under `ui/doctor/` and follows UI → ViewModel → Repository →
DataSource. Planned classes are `DoctorViewModel`, `DoctorResultActivity`,
`DoctorHistoryActivity`, `DoctorHistoryAdapter`, `DoctorRepository`,
`GeminiDoctorAnalyzer`, `DoctorHistoryDao`, and `PlantDiagnosisResult`.
Local photos everywhere are decoded by `util/Bitmaps.decode` (ImageDecoder: upright, scaled while decoding).
`DoctorHistoryEntity` uses the exact eight-field `doctor_history` Room
contract, a nullable Plant foreign key with `ON DELETE SET NULL`, plantId
index, current-UID scoping, and an explicit v1→v2 migration without
destructive fallback. Android UI never calls a provider or DAO directly.
The provider is `data/remote/GeminiDoctorAnalyzer` (Firebase AI Logic) with
`DoctorAiParser` and `DoctorAiException`; Room v3 adds the identification
columns. See `DOCTOR_AI_AND_LANGUAGE.md`.

## Planned Profile and Settings structure

Profile remains `ui/profile/ProfileFragment.java` and the existing fifth
primary destination. Add `EditProfileActivity.java` and its ViewModel under
`ui/profile/` when implemented; load/update the current user through
`UserRepository` and `FirebaseUserDataSource`, with no UID Intent input.
`EditProfileActivity` edits only displayName, locationText, and bio; data
validation and the exact six-field User schema are frozen in
`DATA_MODEL.md`.

Settings uses `ui/settings/SettingsActivity.java` and
`ui/settings/SettingsFragment.java` with `PreferenceFragmentCompat`; its rows use
`item_preference_{top,middle,bottom,single}.xml` and `item_preference_category.xml`.
`ui/settings/AboutActivity.java` is informational. Password reset uses
`ui/auth/ForgotPasswordActivity.java` and `ui/auth/PasswordResetViewModel.java`. A small
`data/preferences/SettingsRepository.java` or `UserPreferences.java` owns the
UID-scoped SharedPreferences namespace and the single current MVP preference,
`care_notifications_enabled` (Boolean, default true). UI does not provide an
arbitrary UID. System notification settings and About/version rows are
actions/information, not stored preferences. Do not add theme, social
notification, sync, or backup controls.

The preference is separate from per-Plant Room reminder fields. A later
notification integration updates `CareReminderScheduler`,
`CareReminderReceiver`, and `GardenRepository` to suppress/cancel alarms when
disabled and reconcile future schedules when enabled, without changing Room
Plant schedule values. The receiver checks authenticated UID and preference;
Android notification permission remains independent. Settings adds no Room
entity, Firestore field, or ERD business entity.

## Planned Safety structure

Safety follows UI → ViewModel → SafetyRepository → FirebaseSafetyDataSource. The planned data classes are model/Report.java, ReportTargetType.java, and ReportReason.java; the remote/repository classes are data/remote/FirebaseSafetyDataSource.java and data/repository/SafetyRepository.java. The remote source owns Firestore report and private block-reference operations. UI never reads or writes Firestore directly, and RecyclerView adapters never query blocked-user data.

Under ui/safety/, the planned pieces are ReportBottomSheet, BlockedUsersActivity, BlockedUsersViewModel, and a small blocked-member adapter as needed. A reusable report sheet receives target type, target ID, and parent ID from its contextual host. Conversation reporting targets the peer USER. The Blocked Users screen resolves canonical User profiles through UserRepository; missing profiles use “PlantMate member” and do not prevent unblock.

Safety entry points are contextual in Listing Detail, Post Detail, Comment/reply, and Conversation; Profile or Settings opens Blocked Users. Firestore rules validate report target existence and self-report constraints, keep reports write-only to ordinary clients, keep each block list private to its owner, and deny block updates. Conversation and Message create rules check both directional block documents. Existing participant message reads remain allowed after a block. Shared Listing/Post/Comment reads remain authenticated community reads; current-blocker content filtering is performed through the Safety repository and feature/ViewModel list transformations. The MVP does not promise that a block hides the blocker's shared content from the blocked member.

## Shared photo step

`ui/photo/ImageCropActivity` (with `CropImageView`) sits between the system photo picker and every
screen that uses a photo: square for plants and listings, 3:2 for posts. Callers use the
`ImageCropActivity.Crop` result contract; several listing photos are cropped one after another
(queue kept in `CreateListingViewModel`). Results are app-private files under `filesDir/photos/`.

## Sign-in providers

Email/password, Google (`GoogleSignInHelper`, Credential Manager) and Facebook
(`FacebookSignInHelper`, Facebook Login SDK). The Facebook SDK starts only when its button is
used (auto-init, app events and advertising ID are off in the manifest), and the button is hidden
until `res/values/facebook.xml` is filled in.

## Build and tests

- The release build runs R8 (`optimization { enable = true }` in `app/build.gradle.kts`) with `app/proguard-rules.pro` (line numbers kept for crash reports, custom view constructors kept) and `res/raw/keep.xml` (keeps `default_web_client_id`, which Google sign-in looks up by name).
- Firebase Crashlytics (free on the Spark plan) reports crashes; release builds upload the R8 mapping file so stack traces stay readable. Raise `versionCode` for every build handed to testers or Play.
- Activity layouts do not paint the page colour on their root: the theme's window background already does (lint `Overdraw`). Fragment roots keep their background for tab transitions.
- Unit tests (`app/src/test`) run with `gradlew :app:testDebugUnitTest`.
- Firestore rules tests (`firestore-tests/rules.test.mjs`, JavaScript by approved exception) give every collection allow and deny cases against the local Firestore Emulator under the demo project `demo-plantmate` (no real data). Run `npm install` once, then `npm test` in `firestore-tests/`; it starts the emulator, runs the tests and shuts the emulator down (firebase-tools leaves it running on Windows otherwise). `RULES_FILE=<path> npm test` runs them against another copy of the rules.
- Instrumented tests (`app/src/androidTest`) cover `DiffList`, the Register and Forgot password validation, the Room 3→10 migrations, plant deletion, the photo crop step and `ui/MainJourneysTest`: six read-only journeys on the signed-in device (five tabs in order; Home give-away → its listing detail → Back; Garden plant → its detail → Back; plant → prefilled Give Away form; a Community category tab; Profile → Friends → Back). The journeys skip without a signed-in account, mark the first-run steps done for the run and restore them afterwards, and never post, send, save or delete. Buttons that late-loading content can move are pressed with `JourneyTestSupport.tap()` (performClick). They never submit valid credentials, so no account is created and no email is sent. Run them without uninstalling the app (Gradle's `connectedAndroidTest` uninstalls it afterwards, which wipes the Room data on the device):

  ```text
  gradlew :app:installDebug :app:assembleDebugAndroidTest
  adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am instrument -w com.example.ungdungchiasecaycanh.test/androidx.test.runner.AndroidJUnitRunner
  ```
