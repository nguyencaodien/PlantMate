# Navigation

## Screen inventory

```text
SplashActivity (launcher)
  ├─ MainActivity (signed in)  ·  CompleteProfileActivity (signed in, no profile)
  └─ SignInActivity (signed out)
SignInActivity
  ├─ RegisterActivity
  ├─ ForgotPasswordActivity
  └─ MainActivity
       ├─ HomeFragment
       ├─ GardenFragment
       ├─ DoctorFragment
       ├─ CommunityFragment
       └─ ProfileFragment
```

Primary tabs, in fixed order: **Home · Garden · Doctor · Community · Profile**.
The bar is docked to the bottom (nav canvas B, 2026-10-21): every tab shows its icon and label, the
selected tab turns forest (lime at night) with a filled icon. Doctor is a raised round ink button
(lime icon; lime with a dark icon at night) over its slot; tapping it opens the Doctor tab straight into its camera (Back returns to the Doctor
tab). There
are exactly five destinations.

System Back on a primary tab (2026-10-14): from Garden, Doctor, Community or Profile it returns to
Home; on Home the first Back shows "Press Back again to exit" and a second Back within two seconds
closes the app.

Secondary Activities: `ListingDetailActivity`, `CreateListingActivity`,
`ListingPreviewActivity`, `SavedListingsActivity`, `MyListingsActivity`,
`PlantDetailActivity`, `AddPlantActivity`, `EditPlantActivity`,
`PostDetailActivity`, `CreatePostActivity`, `InboxActivity`,
`ConversationActivity`, `UserReviewsActivity`, `DoctorHistoryActivity`,
`DoctorResultActivity`, `DoctorScanActivity`, `EditProfileActivity`, `SettingsActivity`,
`AboutActivity`, `BlockedUsersActivity`, `FriendsActivity`, `CreateStoryActivity`,
`StoryViewerActivity`, `OnboardingActivity`, `DeleteAccountActivity` (Settings → Delete account;
on success it opens `SignInActivity` with a cleared task), and `ForgotPasswordActivity`. Care history is part of
`PlantDetailActivity`.

`OnboardingActivity` opens once per account (on that device) on top of `MainActivity`: area (saved
to the profile), what the member came for (receive / give / trade / care; Home shows trade requests
first for members who trade but do not want to receive), and care reminders (asks for the
notification permission on Android 13+). Skip, Back on the first step and finishing all mark it
done; it never stays in the Back stack.

**Launch (2026-10-15, approved mockup v3 A):** `SplashActivity` is the launcher. On the logo's deep green a
light ring is drawn, the logo blooms inside it and the name appears (about 1.6 s; a still frame for 0.4 s
with reduced motion) while the saved session is checked, then it
opens Home, Complete profile or Sign in and finishes, so it never stays in the Back stack. It runs only
on a cold start; signing in, registering or completing a profile goes straight to Home (the old
welcome curtain after sign-in is gone). On Android 12+ its theme blanks the system splash icon so no
logo shows before the intro; the system window is the same deep green.

From `PlantDetailActivity`, "Give or trade this plant" opens `CreateListingActivity` with
`EXTRA_PLANT_ID`; the form reloads the plant from Room and prefills a Give Away listing (name,
species, light, cover photo) without touching the saved listing draft.
PlantMate AI and Sync Center have no active MVP screen.

## Major flows

```text
Home → Listing Detail → Ask / Contact → open or create Conversation → Conversation
Profile → Inbox → Conversation
Home/Profile → Create Listing → Preview → Publish image-less Firestore Listing → Listing Detail
Community → Post Detail → Comments
Listing Detail → Share to Community → Post Detail → Listing Detail
Garden → Plant Detail → Water Now / Add Care Log → Room update
Room schedule → AlarmManager → BroadcastReceiver → Notification → Plant Detail
Inbox → Conversation
Home bell → Notifications → Plant Detail / Garden tab / Post Detail / Member Profile
```

Inbox and Conversation are secondary Activities. Inbox queries only
Conversations whose `participantIds` contain the authenticated UID and opens
the selected screen with `EXTRA_CONVERSATION_ID` only. Profile provides the
Inbox entry; Inbox is not a sixth bottom-navigation tab.

On Listing Detail, an authenticated non-owner may contact the owner. The
ViewModel/repository opens or creates the deterministic Conversation
`{listingId}__{requesterId}` and reuses it on repeated contact. Listing Detail
passes the resulting `conversationId` only to `ConversationActivity`.
Owners do not receive an actionable contact-owner control on their own
Listing. ConversationActivity reloads the Conversation, canonical Listing,
peer User, and messages from that stable ID.

Home is an authenticated Firestore listing feed using RecyclerView with
`ALL`, `GIVE_AWAY`, `TRADE`, and `SELL` filters; `ALL` is the default. Tapping
an item opens `ListingDetailActivity` with `EXTRA_LISTING_ID` only. Home also
provides a Create Listing entry, which is not a bottom-navigation tab.

Listing Detail loads by listing ID, loads owner identity through
`users/{ownerId}`, and supports Save, Share to Community, and the contextual
contact/Ask action. Create Listing collects the approved form, previews it,
and publishes after validation. While remote image upload is unavailable,
publication writes the required `imageUrls` field as `[]`; Listing screens
show the local PlantMate placeholder. Preview-to-publication wiring is the
next implementation step and does not require Firebase Storage.

Registration collects Full Name (displayName), email, password, and Neighborhood / City (locationText). It does not ask for confirm password or location permission. After Auth account creation, it creates the matching `users/{uid}` profile and opens MainActivity only when both writes succeed; profile-write failure follows the documented best-effort Auth rollback.

The Register screen presents a Back action, “Join the Garden” heading,
supporting subtitle, Full Name, Email, Password with “At least 8 characters”
guidance, Neighborhood / City, Create Account, a UI-only Continue with Google
action, and “Already have an account? Sign In.” The location field accepts
manually entered coarse area text.

Trade, Give Away, and Sell conversations begin from a listing context. Give Away can progress through reserve and rehome states; no payment flow exists in MVP.

Each Conversation is one-to-one between the Listing owner and the first
authenticated requester, with exactly two participants. It may be created
before the first message and show an empty-chat state. MVP messages are TEXT
only; messages are immutable after creation. Existing Conversation history
remains available to its participants when the Listing is RESERVED, REHOMED,
or UNAVAILABLE.

Friends and stories (2026-10-09): Profile › Friends and the strip's "Add friends" open
`FriendsActivity` (Friends / Requests); a row opens `MemberProfileActivity` (stable member UID),
whose "Add friend" button sends, accepts, cancels or ends a friendship. Community's story strip,
and "New story" in the "What do you want to post?" sheet, open `CreateStoryActivity`, which
finishes after publishing. A story bubble opens `StoryViewerActivity` with the strip's story IDs
and the start position; the viewer reloads each story through its ViewModel.

## Back and Up behavior

- Bottom-tab changes remain within `MainActivity`; Back follows normal task history rather than creating duplicate tabs.
- A contextual Activity returns to the screen that opened it. Up returns to its logical parent screen.
- After successful listing publish, return to the published listing and remove the creation form from Back history to prevent duplicate publishing.
- Logout clears the authenticated task history and returns to `SignInActivity`.

## Intent ID rules

Pass only stable IDs in Intent extras: `EXTRA_LISTING_ID`, `EXTRA_PLANT_ID`, `EXTRA_POST_ID`, `EXTRA_CONVERSATION_ID`, and `EXTRA_DRAFT_ID` as applicable. Destinations reload data through their ViewModels. Do not pass large domain objects, bitmaps, or duplicate listing snapshots.

Future Review navigation remains secondary: an eligible participant reaches
“Rate this member” from Conversation after the referenced Listing is
`REHOMED`. Review screens receive stable Listing/member IDs and reload their
data through a ViewModel. Listing Detail does not offer Reviews to arbitrary
visitors. A future `UserReviewsActivity` lists Reviews where
`revieweeId == targetUserId`; Reviews do not add a bottom-navigation tab.

Community navigation remains within the five-tab structure:

```text
CommunityFragment
  ├─ CreatePostActivity(optional listingId)
  └─ PostDetailActivity(postId)
       └─ Listing context → ListingDetailActivity(listingId)
```

CreatePostActivity receives only an optional `listingId` and reloads the
canonical Listing. A new share requires an `ACTIVE` Listing and creates a
`SHARING` Post; the Post author need not own it. Post Detail receives only
`postId` and reloads Post, author, optional Listing, Likes, Comments, and
one-level replies. Community remains a primary tab; no additional Community
root Activity is introduced.

Garden is private local data rooted at GardenFragment. Its future secondary
screens are AddPlantActivity, PlantDetailActivity, and EditPlantActivity; they
pass only a local plantId and reload through a ViewModel and GardenRepository.
The repository verifies that the Plant belongs to the current authenticated
Firebase UID before display or CareLog access. Garden does not add a tab,
Firestore collection, or logout data deletion behavior. Plant creation accepts
nickname, optional species, optional local image URI, and optional watering
reminder configuration; marketplace fields, location, owner display name, and
Doctor diagnosis are not Plant inputs.

## Profile and Settings flows

ProfileFragment remains the Profile bottom-navigation destination and hub for
Inbox, My Listings, Saved Plants, My Reviews, Edit Profile, and Settings. It
shows canonical `displayName`, coarse `locationText`, optional `bio`, and the
supported avatar or local placeholder. Profile statistics are not fabricated.

```text
ProfileFragment
  ├─ EditProfileActivity (no UID extra)
  │    └─ load/update current users/{uid} through ViewModel + UserRepository
  └─ SettingsActivity (no UID extra)
       ├─ SettingsFragment / PreferenceFragmentCompat
       ├─ BlockedUsersActivity
       ├─ AboutActivity (informational: intent, expectations, licences)
       └─ AdminHomeActivity (only for admins/{uid}; see ADMIN_FLOW_DESIGN.md)
            └─ AdminListActivity (kind) → AdminListingEdit / AdminPostEdit /
                 AdminMember / AdminReport (listingId / postId / uid / reportId)
```

Edit Profile has LOADING, CONTENT, ERROR, and SAVING states. It edits
`displayName`, `locationText`, and `bio`; failure preserves entered values
and permits retry, while success finishes with `RESULT_OK` so Profile reloads
the canonical document. UID is resolved below UI from Firebase Authentication.
The current profile is not passed through Intent.

Settings requires an authenticated user and resolves that user's
SharedPreferences namespace internally. Its sole functional MVP preference
is `care_notifications_enabled`, default `true`. Values survive logout and
remain isolated across accounts on one device. A Notification settings row
opens Android system notification settings; the app-version row is
informational and the About row opens the informational `AboutActivity`
(bundled font OFL texts and library licences).

Sign in links to `ForgotPasswordActivity`, which sends a Firebase
password-reset email through `PasswordResetViewModel` → `AuthRepository`.
The typed email may be prefilled through the `email` extra. The result
screen never reveals whether an account exists for the address. Settings is secondary and does not add a bottom-navigation
tab.
Doctor remains a primary `DoctorFragment` tab. Planned secondary screens are
`DoctorResultActivity` and `DoctorHistoryActivity`; history navigation passes
only `historyId`, and destinations reload through the authenticated local
repository. Image selection uses a persistable local URI. Provider-unavailable
or error states do not fabricate or save diagnosis results, and Doctor adds
no sixth tab or Firestore collection.
Doctor takes photos with its own camera, `DoctorScanActivity` (CameraX live
preview in a framing window, live light and sharpness checks, flash, lens switch
and the library). It returns the photo URI as its result; without the camera
permission it explains why and keeps the library available. Plant detail's
"Scan for disease" opens the Doctor tab. After a result opens, the form returns
to READY so Back never reopens the result.
After sign-in or registration, `MainActivity` receives `EXTRA_WELCOME` and
plays a short Mầm curtain before creating Home; the sign-in screen is still
finished, so Back leaves the app. See `DOCTOR_AI_AND_LANGUAGE.md`.

## Safety flows

Safety remains contextual and does not add a bottom tab. Profile or Settings can open the secondary Blocked Users screen; it loads the current member's private block references and supports unblock. Report forms are reusable contextual sheets whose host supplies target type, target ID, and parent ID:

- ListingDetailActivity → Report Listing / Block owner
- PostDetailActivity → Report Post / Block author (someone else's post); Delete post (own post, 2026-10-21: confirmed, removes the post with its comments and likes in one batch, then closes and Community reloads)
- Comment or reply → Report Comment / Block author
- ConversationActivity → Report peer User / Block or Unblock peer User
- Profile or Settings → BlockedUsersActivity

Conversation reports target the peer USER, not a Message or Conversation. Listing reports use an empty parent ID; Post reports use an empty parent ID; Comment/reply reports use the containing Post ID. A successful report simply confirms submission and does not promise moderation or automatically block. Blocking requires confirmation. Unblock deletes the current member's own block reference.

Home, Saved Listings, Listing Detail, Community Posts, and Comments/replies may filter content from members in the current user's block set. This is blocker-side filtering only; it does not guarantee reciprocal hiding from the blocked member. Existing Inbox Conversations remain visible and readable. If either-direction block prevents a new Conversation or Message write, show neutral “Messaging is unavailable for this conversation” copy without disclosing which member created the private block.

## Sign in with Google and public profiles (2026-09-26, approved)

- `SignInActivity` › **Continue with Google** uses Credential Manager (`ui/auth/GoogleSignInHelper`,
  `GetSignInWithGoogleOption`) → Google ID token → `FirebaseAuth.signInWithCredential`.
  - Existing `users/{uid}` profile → `MainActivity`.
  - No profile yet → `CompleteProfileActivity` (name prefilled from Google, area required), then
    `MainActivity`. Back or "Use a different account" signs out and returns to `SignInActivity`.
  - A session left without a profile is caught by `checkExistingSession` and sent to the same step.
  - Sign-out also clears the Credential Manager state so the account picker shows next time.
  - Setup: enable the Google provider in Firebase Authentication, add the app's SHA-1/SHA-256,
    and download the new `google-services.json` (it provides `default_web_client_id`).
- `MemberProfileActivity` (uid, context) opens from a post author (tab Posts) or a listing's
  owner row (tab Listings): identity, stats, trust level (`Reputation`) with stars, and tabs
  Listings / Posts / Reviews. It reloads everything by uid.
