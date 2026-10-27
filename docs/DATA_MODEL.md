# Data Model

Status: implementation reconciliation, 27 October 2026 (issue #42).
This document describes the supplied Android source and `firestore.rules`, not
proof of what is deployed. Historical milestones below are not competing
current schemas. This reconciliation does not approve additional MVP scope or
change a schema, ID, source of truth, or Firebase deployment.

## Current sources of truth

| Data | Current source | Evidence |
|---|---|---|
| Session and credentials | Firebase Authentication | `data/remote/FirebaseAuthDataSource.java` |
| Shared profiles, listings, community, chats, reviews, social and moderation data | Firestore | `firestore.rules`, `data/remote/*DataSource.java` |
| Shared photo bytes and thumbnails | Firestore `images` / `imageThumbs`; references `pmimg://<id>` | `FirebaseImageDataSource`, `RemoteImageLoader` |
| Private chat attachments | Firestore `chatMedia`; references `pmchat://<id>` | `FirebaseChatMediaDataSource`, rules |
| Garden, care logs, plant photos, Doctor history, listing drafts, story views | Room `plantmate.db`, version **10**, six entities | `data/local/db/PlantMateDatabase.java`, `data/local/entity/`, DAOs |
| Small preferences and local chat seen times | Account-scoped preferences; app locale is device/app configuration | Settings repository and AppCompat |

Java source paths above are relative to
`app/src/main/java/com/example/ungdungchiasecaycanh/`.
Firestore's SDK cache is not a Room replica or a second authoritative schema.
Firebase Storage is not used for current shared uploads.

## Decisions still requiring team confirmation

| Older approved wording | Observed implementation | Required decision before further implementation |
|---|---|---|
| Six profile fields; immutable/unsupported avatar | Six required fields plus three optional verification flags; preset/owned-photo avatars | Reconcile the overview and admin docs; demo phone/VNeID flags must not imply real identity verification. |
| Remote uploads deferred because Storage is unavailable | Firestore photo bytes, thumbnails and chat media are implemented | Confirm storage/privacy/size policy; no Storage migration is authorized by this document. |
| Listing-specific text-only chat, immutable context | Sorted member-pair chat, direct chat and switchable listing context; TEXT/IMAGE/VOICE/LISTING | Confirm scope against `PLANTMATE_SYSTEM_DESIGN.md` and `MVP_SCOPE.md`; retain legacy IDs. |
| Review proves a completed exchange | Pair existence or matching legacy conversation plus REHOMED listing is sufficient | Evidence does not prove a message, handover, or accepted partner; do not invent an Exchange entity or accepted-participant field. |
| Room v1/v2/v5/v8/v9; eleven-field Plant | Room v10, thirteen-field Plant, twelve-field DoctorHistory | These versions are migration history; other design docs still need reconciliation by their owners. |
| Secure backend/Cloud Function for AI | Existing `GeminiDoctorAnalyzer` uses Firebase AI Logic | Confirm the already documented provider choice against the overview; no provider or secret change here. |

The current #34 rules change validates profile-create avatars and blocks new
chatMedia across either-direction blocks. #43 bounds admin child deletion to
400 writes per batch, then atomically deletes the parent and records the audit.
Neither change expands data fields or UI scope.


## Domain models

| Model | Responsibility |
|---|---|
| User profile | Public-in-app member profile stored in Firestore. |
| Listing | Shared Give Away, Trade, or Sell record. |
| Post | Community content, optionally linked to a listing. |
| Comment and Like | Community interaction records. |
| Conversation and Message | One member-pair discussion, optionally carrying listing context. |
| Plant and CareLog | Personal garden and care history. |
| DraftListing | Local unpublished listing draft. |
| DoctorHistory | Implemented local diagnosis history. PendingSync remains deferred and is not a Room entity. |

## Firestore user profile

The canonical user document is `users/{uid}`, where `uid` is the Firebase
Authentication UID. No separate profile identifier and no duplicate `uid`
field are created.

| Field | Type | Required | Purpose | Source |
|---|---|---|---|---|
| `displayName` | String | Yes | Trimmed, nonblank public-in-app display name; maximum 60 characters. | Registration or profile edit input. |
| `locationText` | String | Yes | Public-in-app coarse neighborhood/city; trimmed, non-blank, max 100 characters. | Manually entered at registration initially. |
| `avatarUrl` | String | Yes | `""`, `preset:<key>`, or owned `pmimg://<id>` reference. | Initials, built-in avatar or Firestore image. |
| `bio` | String | Yes | Trimmed public-in-app short biography, maximum 500 characters; empty string default is valid. | Profile edit input or empty default. |
| `createdAt` | Firestore Timestamp | Yes | Profile creation time. | Firestore server timestamp. |
| `updatedAt` | Firestore Timestamp | Yes | Latest profile update time. | Firestore server timestamp on update. |

These are the six **required** fields. The current model/rules also allow:

| Optional field | Current meaning and write constraint |
|---|---|
| `emailVerified` | Auth-token email verification; on create must match `email_verified`; on update an unchanged value is preserved, a changed value must match the token. |
| `phoneVerified` | Demo-only phone step; absent/false initially, member may switch to true but cannot switch back. |
| `vneidVerified` | Demo-only VNeID step; absent/false initially, member may switch to true but cannot switch back. |

The latter two are not external verification evidence. No extra profile fields
are allowed by the current rules. Email, password,
confirmPassword, auth token, duplicate UID, username, role, latitude,
longitude, exact address, theme, language, notification settings, AI
preferences, sharing radius, follower counts, and ratings do not belong in
this document. Authentication email remains owned by Firebase Authentication;
app preferences remain in SharedPreferences. Username is not an MVP field and
is deferred as **PHASE 2 / TBD** because uniqueness requires an additional
conflict design.

`locationText` is manually entered coarse area information such as
`Quận 7, TP.HCM`, `Thủ Đức, TP.HCM`, or `Hà Nội`. It must be trimmed,
non-blank, and no longer than 100 characters. It is public-in-app profile
data. Registration does not require location permission and must not collect
GPS coordinates, latitude/longitude, a street address, or a home address.

The document is public-in-app profile data: authenticated PlantMate users may
read it when displaying listing, community, or author information. It is not
unauthenticated public Internet data, and no private account data is stored in
the MVP document.

## Firestore collections

```text
users/{uid}
users/{uid}/savedListings/{listingId}
users/{uid}/friends/{friendUid}
friendRequests/{fromUid}_{toUid}
stories/{storyId}
listings/{listingId}
posts/{postId}
posts/{postId}/likes/{uid}
posts/{postId}/comments/{commentId}
conversations/{conversationId}
conversations/{conversationId}/messages/{messageId}
users/{uid}/blockedUsers/{blockedUid}
users/{uid}/notifications/{notificationId}
images/{imageId}
imageThumbs/{imageId}
chatMedia/{mediaId}
reviews/{reviewId}
reports/{reportId}
doctorFeedback/{feedbackId}
doctorFeedbackPhotos/{photoId}
stories/{storyId}/likes/{uid}
admins/{uid}
reportReviews/{reportId}
suspendedUsers/{uid}
adminActions/{actionId}
```

The last four collections belong to the admin contract in `ADMIN_FLOW_DESIGN.md`:
- `admins` is an allowlist bootstrapped in Console; existing admins can grant/revoke other members through the app.
- `reportReviews` records the review status of a report without changing its seven fields.
- `suspendedUsers` blocks content creation.
- `adminActions` is the append-only audit log.

Admins may edit or delete listings, edit or delete posts, and delete comments and likes. They may also edit a member's public profile text. `users/{uid}` keeps six required fields and the three optional verification flags above.

Firestore holds shared records and compressed shared media. See the explicit
schemas below; Firebase Storage is not their current source.

## Firestore listings

The canonical listing document is `listings/{listingId}`. `listingId` is the
Firestore document ID only and is reused by Home, Listing Detail, Community
Plant Sharing, Conversations, and Saved Listings. It is not duplicated as a
document field.

| Field | Type | Required | Contract |
|---|---|---|---|
| `ownerId` | String | Yes | Firebase Auth UID; immutable and never form input. |
| `nickname` | String | Yes | Trimmed, non-blank, maximum 60 characters. |
| `species` | String | Yes | Maximum 100 characters; empty default when unknown. |
| `description` | String | Yes | Trimmed, non-blank, maximum 1000 characters. |
| `type` | String enum | Yes | `GIVE_AWAY`, `TRADE`, or `SELL`. |
| `price` | Integer/long VND | Yes | Zero for Give Away/Trade; greater than zero for Sell. |
| `lookingFor` | String | Yes | Required and non-blank up to 200 characters for Trade; empty otherwise. |
| `imageUrls` | List<String> | Yes | 0–5 `http(s)` URLs or `pmimg://<id>` references. An empty list means no remote image; index 0 is the cover when present. |
| `locationText` | String | Yes | Manually entered approximate area, trimmed and non-blank, max 100 characters. |
| `status` | String enum | Yes | `ACTIVE`, `RESERVED`, `REHOMED`, or `UNAVAILABLE`; starts `ACTIVE`. |
| `createdAt` | Firestore Timestamp | Yes | Immutable server timestamp. |
| `updatedAt` | Firestore Timestamp | Yes | Server timestamp on edits/status changes. |
| `condition` | List<String> | No | Subset of `HEALTHY`, `WITH_POT`, `NEW_LEAVES`, `ROOTED`, `NEEDS_CARE`; written only when chosen. |
| `heightCm` | Integer | No | 1–300; written only when given. |
| `light` | String enum | No | `SUN`, `BRIGHT_INDIRECT`, or `SHADE`; written only when chosen. |
| `pickupTimes` | List<String> | No | Subset of `SAT_MORNING`, `SUN_AFTERNOON`, `WEEKDAY_EVENING`, `WEEKDAY_DAYTIME`; written only when chosen. |
| `category` | String enum | No | `DESK_LEAFY`, `SUCCULENT`, `HANGING`, `FLOWERING`, `HERB`, `BONSAI`, or `OTHER`; written only when chosen. |
| `potCm` | Integer | No | Pot diameter, 1–100; written only when given. |
| `age` | String enum | No | `UNDER_3_MONTHS`, `MONTHS_3_TO_12`, or `OVER_A_YEAR`; written only when chosen. |
| `negotiable` | Boolean | No | Sell only: the price can be discussed; never written for Give Away or Trade. |

**Update 2026-10-07 (approved, "make canvas blocks real features"):** the four
optional detail fields above describe the plant and when the owner is free to
hand it over (listing detail "Plant condition" and "Pick-up"). Older listings
have none of them; unknown stored values are skipped when read. Firestore rules
allow them through `listingDetailFields()` / `validListingDetails()`; the rule
change must be deployed with the Firebase CLI before clients write these fields.

**Update 2026-10-14 (approved, canvas B create and detail screens):** `category`,
`potCm`, `age` and `negotiable` join the optional details (create listing "Loại
cây", "Kích thước & chậu", "Tuổi cây", "Có thể thương lượng"; listing detail facts
grid). The create form no longer asks for `light`; older listings keep and still
show it. Rules validate the four new fields in `validListingDetails()`; the rules
tests cover them, and the updated `firestore.rules` must be deployed before
clients write them.

Give Away forces `price = 0` and `lookingFor = ""`; Trade forces `price = 0`
and requires `lookingFor`; Sell requires `price > 0` and forces
`lookingFor = ""`. No currency, payment, shipping, exact address, GPS, or
care fields belong in the MVP listing.

Authenticated users may read listings. Only the owner may create; owner status updates and admin edits preserve
`ownerId` and `createdAt`. Admin or owner deletion is allowed (including account cleanup).
Allowed transitions are ACTIVE to ACTIVE/RESERVED/REHOMED/UNAVAILABLE,
RESERVED to RESERVED/ACTIVE/REHOMED/UNAVAILABLE, UNAVAILABLE to
UNAVAILABLE/ACTIVE, and REHOMED to REHOMED. REHOMED is terminal.

`imageUrls` is always present, never null. `[]` is valid and renders a local
placeholder. Current upload writes compressed JPEG/base64 into `images/{id}`
and stores only `pmimg://<id>` in the listing. Existing HTTP(S) images remain
supported. Never store local URIs, inline base64 or fake placeholder URLs in
Listing/Post fields.

### Shared images and thumbnails

| Path | Fields | Bounds and ownership |
|---|---|---|
| `images/{imageId}` | Required `ownerId`, `data`, `width`, `height`, `createdAt`; optional `audience` | Rules: base64 length 101–700000; dimensions integer 1–1600; own UID, server timestamp, not suspended. |
| `imageThumbs/{imageId}` | Required `ownerId`, `data`, `createdAt`; optional `audience` | Rules: length 101–120000; matching original image owner and audience; no width/height fields. |

`audience` defaults to `public` (authenticated-in-app), or is `friends` for
story photos. Get uses that audience: owner/friends/admin for friends-only
photos; list is owner-filtered or admin. Both collections deny update and
allow owner/admin deletion. Thumbnail IDs equal original image IDs.
The uploader targets 1080 px and 650000 base64 characters; this is a client
target, not a guarantee for its fallback compression path (tracked in #54).
Thumbnail upload is best effort; readers fall back to the full image.
Base64 bytes are in the media document, never in the reference field.

## Firestore conversations and messages

Current messaging is one conversation per member pair, with optional listing
context. Inbox remains a secondary Activity. Canonical paths are
`conversations/{conversationId}` and its `messages/{messageId}` subcollection.

### Conversation IDs and fields

New IDs are `{uidA}__{uidB}` where UIDs are sorted lexicographically; the
`participantIds` list uses that order. `ChatRepository.pairConversationId`
and `firestore.rules:isPairConversation` implement the same convention.
Existing `{listingId}__{requesterId}` IDs are legacy: they remain readable and
accept messages/summary updates, but new creates require a pair ID and legacy
chats cannot switch listing context. No migration renames existing documents.

Exactly seven fields are stored; the document ID is not a field:

| Field | Type | Current meaning |
|---|---|---|
| `listingId` | String | Most recently selected listing reference, or `""` for a direct chat started from a profile. |
| `ownerId` | String | Selected listing owner; for an initially direct chat, the other member. |
| `requesterId` | String | Member opening/selecting that context, distinct from owner. |
| `participantIds` | List<String> | Exactly two distinct UIDs; immutable. |
| `lastMessage` | String | Empty initially; latest text/media preview, max 1000 characters. |
| `createdAt` | Timestamp | Immutable server creation time. |
| `updatedAt` | Timestamp | Server time on creation and summary updates. |

Create requires authenticated requester, no suspension/block in either
direction, and a real listing belonging to owner, or an existing other user
for direct chat. Opening direct chat reuses an existing pair without clearing
its listing. Selecting another listing in a pair chat changes only
`listingId`, `ownerId`, `requesterId`: caller must be requester, referenced
listing must belong to the other participant, and no block/suspension is
allowed. This context switch leaves timestamps and summary unchanged.
Ordinary updates change only `lastMessage`/`updatedAt`; these remain allowed
independently of message writes even after a block. Delete is denied.

### Message schemas

All messages require `senderId == auth.uid`, parent membership, no suspension,
no block in either direction, and `createdAt == request.time`.

| Type | Exact fields | Additional validation |
|---|---|---|
| `TEXT` | `senderId`, `type`, `text`, `createdAt` | Text 1–1000 characters; repository trims and rejects blank input. |
| `IMAGE` | Those four plus `mediaRef`, `durationMs` | Reference `pmchat://<id>` owned by sender, same conversation/kind; duration 0, caption max 1000. |
| `VOICE` | Those four plus `mediaRef`, `durationMs` | Matching owned media, empty text, duration 500–60000 ms. |
| `LISTING` | Four base fields plus `listingId` | Empty text; existing listing owned by either participant; no copied listing data. |

Read requires parent membership, including after a block. Update/delete are
denied. `FirebaseChatDataSource` writes a message then updates the conversation
summary separately; summary failure can leave the message successfully stored.
There is no atomic-send guarantee, receipt field or typing field.

### Private chat media

`chatMedia/{mediaId}` has exactly `ownerId`, `conversationId`, `kind`, `data`,
`mime`, `durationMs`, `createdAt`. `kind` is IMAGE/VOICE; `mime` is image/jpeg
or audio/mp4; data length is 101–700000; duration is integer 0–60000.
Create requires own UID, existing conversation membership, no suspension,
no bilateral block and server time. The current media-create rule validates
kind and MIME independently; message validation supplies tighter duration and
kind checks. Do not claim a stricter MIME/kind binding than the rules enforce.
Read is allowed to the owner or a conversation participant; update is denied,
delete is owner-only. Blocking preserves existing media/history read access.

Inbox queries `participantIds array-contains uid` and sorts by updatedAt;
`ChatRepository.withMessages` hides empty conversations. Snapshot listeners are
owned by repository subscriptions and screen/ViewModel lifecycle. Peer profile
and listing details are loaded from canonical documents. Chat seen times are
account/device preferences, not remote read receipts; no FCM guarantee exists.

Conceptually USER N:N CONVERSATION is represented by `participantIds`, without
a participant collection. A conversation has many messages; listing context
is optional and mutable for pair chats. Review evidence is defined below.

## User profile security contract

The current `users/{uid}` rules allow authenticated read and owner-only create.
Create requires the six base fields, permits only the three optional flags
above, and validates display name 1–60, location 1–100 and bio 0–500 characters.
The client trims these strings. Both timestamps equal request time on create.

Avatar create uses `validAvatar`: empty, `preset:[a-z0-9_]{1,24}`, or
`pmimg://[A-Za-z0-9]{10,40}` whose image owner equals caller UID. The rule checks
preset syntax, not membership in the built-in preset list. Owner update can
preserve an unchanged legacy avatar; a changed value must pass `validAvatar`.
Creation time is immutable and updatedAt must be request time. Member updates
can change text, avatar and verification flags under their constraints.
Admin profile edits change only displayName/locationText/bio/updatedAt,
preserving avatar and verification state. Owner deletion is allowed for account
cleanup. The old six-fields-only/no-avatar/no-delete contract is historical.

## Registration and profile editing contract

Registration collects `displayName` (Figma label “Full Name”), `email`,
`password`, and `locationText` (Figma label “Neighborhood / City”).
Confirm Password is not part of the approved Register UX and is not stored.
Registration password is required and has a minimum length of eight
characters; no uppercase, lowercase, number, or special-character rule is
imposed. Sign-in only requires a valid email and a non-empty password, so
registration-only password rules do not reject existing accounts.

After Firebase Authentication account creation succeeds, the application
creates `users/{FirebaseAuth UID}` with normalized `displayName` and
`locationText`, empty `avatarUrl` and `bio`, and server timestamps for
`createdAt` and `updatedAt`. Registration succeeds only after both Auth and
profile creation succeed. If profile creation fails, preserve the best-effort
Auth account deletion and sign-out rollback; do not navigate to MainActivity.
Edit Profile loads the current user's canonical profile through a ViewModel
and repository; the UI and Intent do not supply a UID. It edits only
`displayName`, `locationText`, and `bio`. Display name follows the
registration rule: trimmed, nonblank and at most 60 characters
(`AuthViewModel.DISPLAY_NAME_MAX`; `firestore.rules` enforces the same limit on
create and update).
Location remains manually entered coarse text, trimmed, nonblank, and limited
to 100 characters. Bio is trimmed, may be empty (stored as `""`), and is
limited to 500 characters. The update changes only these three fields and
`updatedAt = FieldValue.serverTimestamp()`; `avatarUrl` and `createdAt` remain
unchanged. Avatar selection/upload is a separate action in Edit Profile; these text-update
fields do not include avatar, email or password. A successful save returns `RESULT_OK`; failure retains form
input and re-enables Save.

**Email verification.** After registration succeeds, the app sends a Firebase
verification email (best effort; a failure never blocks sign-up). Profile
shows a reminder card to email/password members until the address is
confirmed, with "Send again" and "I've confirmed" (which reloads the account).
Google accounts never see it. General membership does not require email verification; SELL creation does,
as described below.

**Password reset** reports success whether or not an account exists for the
address, so the screen cannot be used to discover registered emails.

**Verified sellers.** Creating a `SELL` listing requires a verified email; the
Firestore rules check the sign-in token (`request.auth.token.email_verified`),
and the app refreshes the token before publishing. `users/{uid}` may carry an
optional `emailVerified` boolean, which the member's app publishes once Firebase
confirms the address; the rules accept it only when it matches the token. It
drives the "Email verified" pill on Profile, member profiles and listing
details. It says the email is confirmed, nothing about the person's identity.
Identity checks through VNeID need an official connection to the national
e-ID system and are not part of the app.

**Sell checks (2026-10-17).** `users/{uid}` also carries `phoneVerified` and
`vneidVerified` booleans from two demo steps: the phone screen shows its code on
screen (no SMS, the number is never stored) and the VNeID screen simulates the
approval. The rules let only the member switch them on, never off, and a new
profile must start with both false. Creating a `SELL` listing needs the email
token plus both flags (`get(users/uid)`); the app shows a "Selling needs 3
checks" sheet first. The trust score (0-100) is history 75 (ratings 45,
completed exchanges 20, tenure 10) plus verification 25 (email 8, phone 7,
VNeID 7, photo and bio 3), computed in `Reputation` from public profile fields.

**In-app notifications (2026-10-17).** `users/{uid}/notifications/{id}` holds
`type` (COMMENT, REPLY, MENTION, LIKE), `actorId`, `postId`, `text` (≤ 140
characters, "" for likes), `read` and `createdAt`. The actor's app writes it
(no Cloud Functions on Spark) with a deterministic ID (`{TYPE}_{commentId}`,
`LIKE_{postId}_{actorId}`) so nothing notifies twice; rules require the actor's
own UID, an existing post, a recipient other than the actor, no block in either
direction and `read == false`. Only the recipient reads, sets `read` to true or
deletes; account deletion removes the list. Watering rows come from Room and
friend requests from `friendRequests`; neither is copied into Firestore.

**Chats from a profile and new messages (2026-10-17).** A member can open the
pair conversation from another member's profile ("Nhắn tin"): it is created
with `listingId == ''` (and `ownerId` = the other member) when the pair has none,
otherwise the existing one is reused and keeps its plant. Rules check that the
other member exists and that neither blocked the other. "New messages" has no
read receipts in Firestore: each chat's last-open time is kept in the account's
preferences on the device (`SettingsRepository.getChatSeen`), and a chat counts
as new when its `updatedAt` is later; chats untouched since the feature first ran
count as read. Reading on another device does not clear it here.

**Offline use (2026-10-21).** Two on-device SQLite stores keep the app usable
without a network:
- Room (`plantmate.db`, `PlantMateDatabase`): plants, care logs, Doctor history,
  plant photos, listing drafts and story views. Garden, care, reminders and
  drafts work fully offline; this is the source of truth for that data.
- Firestore's persistent cache (configured in `PlantMateApplication`, 100 MB):
  listings, posts, profiles, conversations and messages read recently are
  served from it offline; anything posted by others since then is not.
  `PlantMateApplication` follows the device network and calls
  `disableNetwork()` / `enableNetwork()` so offline reads come from the cache
  at once instead of after Firestore's ~10 s server timeout. Writes made
  offline are queued and sent on reconnect.
- Aggregate counts (likes, comments, unread, active listings, admin totals) are
  not cached by Firestore; `OfflineCounts` remembers the last successful count
  per signed-in account and returns it offline (0 if never counted).

**Facebook sign-in.** Facebook Login gives an access token that Firebase turns
into an account (`FacebookAuthProvider`), then the Google flow continues
(profile check, Complete profile). The button stays hidden until
`res/values/facebook.xml` holds a real Meta app ID and client token; the App
Secret belongs only in Firebase Console. An email already registered with
another sign-in method gets a clear message instead of a second account.

**Doctor feedback (Firestore).** `doctorFeedback/{id}`: `ownerId`, `correct`,
`diagnosis`, optional `plantName`, `scientificName`, `confidence`, `healthy`,
`correction` (≤ 300, empty when correct), `photoId` (`""` or a
`doctorFeedbackPhotos` ID) and `createdAt`. `doctorFeedbackPhotos/{id}`:
`ownerId`, base64 JPEG `data`, `width`, `height`, `createdAt`. Members can only
create their own; only admins read; nothing is updated or deleted from the app.

**Photos picked in the app** (post, plant, listing) go through a crop step and
are saved as JPEGs in app-private `filesDir/photos/`, served by the same
FileProvider; plants keep that local URI, posts and listings upload it as before.

## Firestore reviews (frozen contract)

Reviews represent directed member-to-member trust after a Listing-related interaction. They are not ratings of a plant, Listing, or arbitrary profile. The canonical path is `reviews/{reviewId}` and the deterministic document ID is `{listingId}__{reviewerId}__{revieweeId}`; this ID is not stored as a field. Opposite directions are separate Reviews.

The document contains exactly six fields: `listingId` (immutable Listing reference), `reviewerId` (authenticated creator), `revieweeId` (distinct recipient), `rating` (integer 1–5), `comment` (required String, trimmed, empty when blank, maximum 500 characters), and `createdAt` (immutable server Timestamp). Create and authenticated read are supported; update is denied; the reviewer may delete their own review for account cleanup.

Creation requires an authenticated, non-suspended reviewer, an existing
`REHOMED` Listing, distinct reviewer/reviewee and the Listing owner as one party.
`validReviewEvidence` accepts existence of the sorted member-pair conversation,
regardless of its current listing (including a direct chat), OR a legacy
`{listingId}__{requesterId}` conversation with matching listing, owner,
requester and exactly those two participants. The repository additionally
checks the pair's participants. Rules do not require any actual message or
match the pair conversation's current listing. The deterministic ID permits one directed Review per Listing/member pair.

The Listing schema does not record an accepted exchange partner. Therefore `REHOMED` plus the pair/legacy evidence above is current eligibility evidence, but multiple requesters cannot be distinguished as the completed partner. A future Exchange entity or accepted-participant field would be needed for stricter attribution; no Listing field is added now.

Review summaries are derived from Reviews queried by `revieweeId`, with client-side newest-first sorting when useful. Do not add `averageRating`, `reviewCount`, or star fields to User. With no Reviews, show “No reviews yet”. Reviewer display data is resolved from User and is not duplicated in Review. The ERD relationships are USER 1—N REVIEW as reviewer, USER 1—N REVIEW as reviewee, and LISTING 1—N REVIEW; Conversation is eligibility evidence only.

## Query limits and indexes

- Home reads the newest `ACTIVE` listings 50 at a time (`HomeViewModel.PAGE_SIZE`, `startAfter`,
  ordered by `createdAt` descending; index `listings (status ASC, createdAt DESC)`), up to 300 in
  all. Older pages load when the full grid nears its end, and by themselves while a type filter,
  search or the filter sheet shows fewer than 20 listings, so filtering and search still cover what
  the old single 300-listing read covered. Area, photo, sort and search filters run on the device.
  While pages remain, the Give Away / Trade / Sell counts come from server `count()` aggregates
  (they include blocked members' and stale listings); once everything is loaded they are counted on
  the device. Doctor's givers and similar listings still use `getActiveListings` (newest 300).
- Home also reads up to 30 Sell listings that are `RESERVED` or `REHOMED`, most recently updated first
  (`type == SELL`, `status in [RESERVED, REHOMED]`, ordered by `updatedAt` descending; index
  `listings (type ASC, status ASC, updatedAt DESC)`). Reserved ones always show and sold ones show for 3
  days after the sale (`ListingRepository.SOLD_SHOWN_MS`), in the grid and the sale row with their status
  pill (Available / Reserved / Sold). The read is best effort: if it fails, Home shows active listings only.
  The hero, urgent and givers sections stay active-only.
- The Community feed loads 20 posts at a time (`CommunityRepository.PAGE_SIZE`,
  `startAfter` the last document) and fetches the next page when the reader
  nears the end. A category chip filters in the query (`category ==`), using
  the composite index `posts (category ASC, createdAt DESC)` in
  `firestore.indexes.json`. Like and comment counts use server-side `count()`
  aggregates for the loaded page only, and author profiles are requested once
  per author. Search and "most liked" work on the loaded pages.
- A member's profile loads their posts with `posts where authorId ==`
  (single-field index, no composite index), not the whole feed.
- Listing lifecycle (no backend): an `ACTIVE` listing whose `updatedAt` is 14 days old asks its
  owner "Is it still available?" on Profile ("Still available" rewrites the status as `ACTIVE`,
  which refreshes `updatedAt`; "Already given" marks it `REHOMED`). After 30 days without an update
  `ListingRepository.getActiveListings` leaves it out of Home, Doctor's givers and similar listings
  until the owner confirms. The owner still sees it in My listings.
- Account deletion (Spark, no backend; `AccountRepository`, `FirebaseAccountDataSource`): after a
  re-authentication (Google first, else password; other providers only right after signing in) the
  app, as the member, queries everything they own (listings, posts, comments via a collection-group
  query on `comments.authorId`, stories, images, image thumbnails, chat media, friend requests both
  ways, reviews written, saved listings, blocks, friendships and each friend's mirror), deletes it in
  batches of 400, deletes `users/{uid}` last, clears the account's Room rows, reminders, private
  photo files and settings, then deletes the Firebase Auth user. Nothing is deleted unless every
  query succeeds; a run that stops partway can be repeated. Kept: messages sent, reviews about the
  member, reports, Doctor feedback, and post / story hearts (keyed by UID with no field to query).
  Rules let a member delete their own profile, listings, posts, comments and written reviews, and
  read their own chat media; the collection-group index is a field override in
  `firestore.indexes.json`.
- Admin moderation still reads the newest 100 posts in one query
  (`FirebaseCommunityDataSource.FEED_LIMIT`).
- A story photo carries `audience: 'friends'` on `images/{id}` and its `imageThumbs/{id}` (2026-10-15,
  QA BUG-001): only the owner, their friends and admins can get it, and a story must use such a photo.
  Other photos have no `audience` and any signed-in member can get them by ID. Members can list only
  their own photos (`where ownerId ==`, used by account deletion); admins can list all.
- `imageThumbs/{imageId}` holds a small JPEG (longest side 320 px, base64
  `data`, `ownerId`, `createdAt`) of `images/{imageId}`. The uploader writes it
  right after the photo (best effort); rules allow it only for the owner of the
  matching `images` document, with no update. Cards and grids
  (`RemoteImageLoader`, display width ≤ 640 px) read the thumbnail and fall back
  to the full photo when it is missing (photos uploaded before thumbnails).

## Friends and stories (approved 2026-10-09)

| Path | Fields | Who can read / write |
|---|---|---|
| `friendRequests/{fromUid}_{toUid}` | `fromId`, `toId`, `createdAt` (server time) | Read and delete: the two members. Create: the sender, not to themself, not across a block, not to a friend, not when the other member already asked. No update. |
| `users/{uid}/friends/{friendUid}` | `createdAt` (server time) | Read and delete: either friend. Create: the member accepting a pending request writes both mirror documents in one batch that also deletes the request. No update. |
| `stories/{storyId}` | `authorId`, `title` (1–24), `caption` (0–200), `imageUrl` (`pmimg://` photo owned by the author), `createdAt` (server time) | Read: the author, their friends, admins. Create: the author (not suspended). Delete: the author or an admin. No update. |
| `stories/{storyId}/likes/{uid}` | `createdAt` (server time) | Read: the story's author (the count) and the member who hearted. Create: a friend of the author, as themself. Delete: that member. No update. Left in place when the story is deleted (unreadable to others once the story is gone). |

Friend suggestions are computed on the device: members you have a conversation with who are not
friends, not blocked and have no pending request, same area (accent- and case-insensitive) first,
at most five, shown under the friends list.

A story is shown for 24 hours after `createdAt`; expiry is applied on the device and the author's
app deletes its own expired stories when it loads the strip. Stories are loaded with one equality
query per author (the member and each friend), so no composite index is needed. Friendship is
symmetric; there are no follower counts and `users/{uid}` has the required and optional fields documented above.

## Room entities

| Entity | Purpose |
|---|---|
| `PlantEntity` | Account-scoped personal plant. |
| `CareLogEntity` | Care event for a plant. |
| `ListingDraftEntity` | Account-scoped unpublished listing draft (table `listing_drafts`, one per account). |
| `PlantPhotoEntity` | A plant's "photos over time" entry (table `plant_photos`). |
| `DoctorHistoryEntity` | Owner-scoped local Doctor history. |
| `StoryViewEntity` | A friend's story this account has opened (table `story_views`, key `ownerUid` + `storyId`). |
| PendingSync (not an entity in v10) | Deferred; absent from the `@Database` entity list. |

The database class is `data/local/db/PlantMateDatabase.java`, version **10**.
The six implemented entities are scoped directly by `ownerUid` or through
their parent plant; CareLog and PlantPhoto do not duplicate the UID.

| Table | Persisted columns in v10 | Key / relationship |
|---|---|---|
| `plants` | `id`, `ownerUid`, `nickname`, `species`, `imageUri`, `reminderEnabled`, `wateringIntervalDays`, `nextWateringAt`, `createdAt`, `updatedAt`, `wateringTimes`, `room`, `light` | Auto ID; owner-scoped. |
| `care_logs` | `id`, `plantId`, `type`, `note`, `createdAt` | Auto ID; indexed plant FK, CASCADE. |
| `plant_photos` | `id`, `plantId`, `imageUri`, `createdAt` | Auto ID; indexed plant FK, CASCADE. |
| `doctor_history` | `id`, `ownerUid`, `plantId`, `imageUri`, `diagnosis`, `confidence`, `guidance`, `createdAt`, `plantName`, `scientificName`, `healthy`, `feedback` | Auto ID; nullable indexed plant FK, SET NULL; owner remains on history. |
| `listing_drafts` | `ownerUid`, `type`, `nickname`, `species`, `lookingFor`, `price`, `description`, `locationText`, `imageUris`, `conditions`, `heightCm`, `light`, `pickupTimes`, `updatedAt`, `category`, `potCm`, `age`, `negotiable` | `ownerUid` primary key; one draft per account, no separate stored draftId. |
| `story_views` | `ownerUid`, `storyId`, `viewedAt` | Composite owner/story key; local seen state, not a story replica. |

Registered migrations are 1→2 Doctor history; 2→3 identification columns;
3→4 wateringTimes; 4→5 feedback; 5→6 room/light; 6→7 plant photos;
7→8 drafts; 8→9 story views; 9→10 draft details. No destructive fallback.
The database currently uses `exportSchema = false`; this document records
source inspection, not a migration execution result. Historical steps follow. `PlantEntity` has
one-to-many relationships with `CareLogEntity`, `DoctorHistoryEntity` and
`PlantPhotoEntity`.

**Update 2026-10-07 (approved):** the schema reached version 8 (historical).
`MIGRATION_5_6` adds `plants.room` and `plants.light` (both `TEXT NOT NULL
DEFAULT ''`; the area groups the Garden "Areas" cards, `light` is a
`LightLevel` name). `MIGRATION_6_7` creates `plant_photos` (`id`, `plantId` →
`plants.id` ON DELETE CASCADE, `imageUri` app-private cropped photo, `createdAt`),
read only through a join on `plants.ownerUid`. `MIGRATION_7_8` creates
`listing_drafts` keyed by `ownerUid` (one saved draft per account: type, text
fields, price, photo URIs one per line, condition and pick-up names as CSV,
height, light, `updatedAt`). "Save draft" writes it, the create form restores
it once, "Discard" deletes it, and publishing clears it. Garden "Today", the
week strip, "This week" and the care streak are computed from `care_logs`
joined to the owner's plants; nothing is invented.

**Update 2026-10-09 (approved):** the schema reached version 9 (historical). `MIGRATION_8_9` creates
`story_views` (`ownerUid`, `storyId`, `viewedAt`; primary key `ownerUid` +
`storyId`). It only greys the ring of stories this account has opened; views
older than two days are deleted when the strip loads.

**Update 2026-10-14 (approved):** Room is version 10. `MIGRATION_9_10` adds
`listing_drafts.category` (`TEXT NOT NULL DEFAULT ''`), `potCm` (`INTEGER NOT
NULL DEFAULT 0`), `age` (`TEXT NOT NULL DEFAULT ''`) and `negotiable` (`INTEGER
NOT NULL DEFAULT 0`), so a saved draft keeps the new listing details.

## SharedPreferences ownership

The watering preference is
`care_notifications_enabled`, a Boolean defaulting to `true`. Its logical key
is stored in a SharedPreferences namespace scoped by the current Firebase
Auth UID, so accounts on one device do not inherit each other's value.
Preferences remain on logout and return for the same account at its next
sign-in. Settings require authentication; missing authentication must never
fall back to another namespace.

This preference is local-only and is not part of `users/{uid}`, Firestore, or
Room. It controls delivery of local watering reminder notifications, not the
per-Plant `reminderEnabled`, `wateringIntervalDays`, or `nextWateringAt`
schedule. Turning it off preserves all Room schedules, cancels/suppresses the
current user's OS alarms where practical, and keeps Garden due/upcoming state
available. Turning it on reconciles only future valid schedules and does not
immediately notify for overdue Plants. `CareReminderReceiver` must check both
the current authenticated UID and this preference before delivery. Android's
`POST_NOTIFICATIONS` permission and notification-channel state remain
separate requirements. A Notification settings action may open Android system
settings but is not another stored preference. App version and About rows are
informational only. The current implementation also has theme and per-chat
seen state; per-app language uses AppCompat. The old claim that watering was
the only functional setting is historical, not a current exhaustive key list.

### Backup

`android:allowBackup="false"`, and `backup_rules.xml` /
`data_extraction_rules.xml` exclude the database, shared preferences and files
from both cloud backup and device transfer. Room data (Garden, care logs,
drafts, Doctor history) and account-scoped preferences therefore never leave
the device through Android backup.

## Values

| Type | Values |
|---|---|
| `ListingType` | `GIVE_AWAY`, `TRADE`, `SELL` |
| `ListingStatus` | `ACTIVE`, `RESERVED`, `REHOMED`, `UNAVAILABLE` |
| `PostCategory` | `SHARING`, `QUESTION`, `PLANT_WIN`, `TIP`, `GENERAL` |
| `MessageType` writes | `TEXT`, `IMAGE`, `VOICE`, `LISTING`; `TRADE_OFFER` and `SYSTEM` are not accepted writes. |

Current code sends text, photo, voice and listing-card messages. Trade-offer and system messages
are not accepted by Firestore writes.

## Source of truth and boundaries

Firebase Authentication is the source of truth for session identity and
email/password credentials. Firestore is the source of truth for the shared
user profile, listings, saved references, posts, likes, comments,
conversations, and messages. Room is the source of truth for personal Garden
data and local listing drafts. SharedPreferences owns small app settings.
Do not duplicate Firestore listings or profiles as conflicting Room records.

## Garden Room local contract

Garden is the authenticated user's private, local-first plant-care area. Its
canonical source is Room; Garden data is not written to Firestore, Firebase
Storage, or a shared collection, and it is not automatically synchronized
between devices. Local rows remain after logout, but every Garden query is
scoped to the currently authenticated Firebase UID so another account on the
same device cannot see them.

The initial Room database is `PlantMateDatabase`, version 1 when first
implemented. No destructive migration policy is part of the contract; later
schema changes use explicit migrations.

### PlantEntity

Table: `plants`. Java class: `PlantEntity`. The current entity contains thirteen
fields (the former eleven-field description predates room/light):

| Field | Type | Required / invariant | Meaning |
|---|---|---|---|
| `id` | `long` | Primary key, auto-generated | Local Room identity; never a Firebase or Firestore ID. |
| `ownerUid` | `String` | Required, non-blank, immutable | Firebase Authentication UID captured at creation; external ownership reference, not a Room User foreign key. |
| `nickname` | `String` | Required after trim, 1–60 chars | User-facing plant name. |
| `species` | `String` | Required field; blank stored as `""`; max 100 chars | Optional species name. |
| `imageUri` | `String` | Required field; blank stored as `""` | Local Android URI such as `content://...`; never remote Listing/Post data. |
| `reminderEnabled` | `boolean` | Required | Whether a watering schedule is active. |
| `wateringIntervalDays` | `int` | `0` when disabled; 1–365 when enabled | Days between watering reminders. |
| `nextWateringAt` | `Long` nullable | `null` when disabled; positive epoch milliseconds when enabled | Absolute local reminder target. |
| `createdAt` | `long` | Required, immutable | Local epoch milliseconds at creation. |
| `updatedAt` | `long` | Required | Local epoch milliseconds after editable plant or reminder changes. |
| `wateringTimes` | `String` | Required; `""` for every-N-days plans (Room v4, default `''`) | Daily plan: 1–4 distinct reminder times as minutes after midnight, sorted, e.g. `"420,1020"` = 07:00 and 17:00. |

`room` and `light` are required Strings defaulting to `""` (v6); room groups
Garden areas and light stores a `LightLevel` enum name. These complete the
thirteen fields listed in the v10 table.

The reminder invariant is strict: disabled means false/0/null; enabled means
true, an interval from 1 through 365, and a non-null positive
`nextWateringAt`. A reminder plan is either every N days (`wateringTimes == ""`)
or several fixed times every day (`wateringTimes` set, `wateringIntervalDays == 1`,
`nextWateringAt` = the next of those times). Garden does not store a plant `status`; presentation-only
watering states such as `NO_REMINDER`, `UPCOMING`, `DUE`, and `OVERDUE`
are derived from the schedule and current time. Diagnosis and health fields
belong to Doctor, not Plant.

### CareLogEntity and CareType

Table: `care_logs`. Java class: `CareLogEntity`. It contains exactly five
fields:

| Field | Type | Required / invariant | Meaning |
|---|---|---|---|
| `id` | `long` | Auto-generated primary key | Local care-event identity. |
| `plantId` | `long` | Required foreign key to `plants.id` | Parent Plant relationship; index this field. |
| `type` | `String` enum | Required | `WATER`, `FERTILIZE`, `REPOT`, `PRUNE`, or `NOTE`; never ordinal values. |
| `note` | `String` | Required field; blank stored as `""`; max 500 chars | Optional except NOTE, where it is non-blank. |
| `createdAt` | `long` | Required | Epoch milliseconds when the action was recorded. |

`CareType` is exactly WATER, FERTILIZE, REPOT, PRUNE, and NOTE. A Plant has
many CareLogs; the implemented Room foreign key uses ON DELETE CASCADE. CareLog
does not duplicate `ownerUid`; ownership is derived through
`plantId -> PlantEntity.ownerUid`, and the repository verifies the current
UID owns the parent before reading or creating logs.

A successful WATER event may recalculate `nextWateringAt` as the water-action
time plus `wateringIntervalDays` when reminders are enabled; for a daily plan
it becomes the first reminder time at least one hour after the watering (so an
early watering covers the coming slot). Other care types do not change the
watering schedule. Care-log editing and deletion are outside the current MVP.

**Watering log limit.** `GardenRepository.recordCare` refuses a WATER log (a
`CareLimitException`, shown as a friendly message) when today already has as
many waterings as the plan (the number of daily times, otherwise one), or when
the last watering was less than an hour ago. The check runs inside the insert
transaction, so the Plant Detail button, the Garden drop button and "Water
all" all obey it. A daily plan whose watering is missed rings again at the
next reminder time.

### Garden ownership and planned operations

The UI never supplies an arbitrary owner UID and never queries Room directly.
The planned chain is Garden UI/ViewModel → `GardenRepository` → `PlantDao`
or `CareLogDao` → `PlantMateDatabase`. Planned Plant operations are
`insertPlant`, `updatePlant`, `getPlantById`,
`getPlantsForOwner(ownerUid)`, and
`getPlantsWithRemindersForOwner(ownerUid)`. CareLog operations are
`insert` and `observeHistory(plantId)`, which returns the log newest first
(`ORDER BY createdAt DESC`). User-facing lists always filter by current UID;
Plant Detail receives only a local `plantId` and rechecks ownership.

**Update 2026-10-10 (approved):** a plant can be deleted from Edit plant
("Xoá cây", with confirmation). `GardenRepository.deletePlant` cancels its
reminder and runs `PlantDao.deleteForOwner(plantId, ownerUid)`; care logs and
plant photos go with it through `ON DELETE CASCADE`, Doctor history keeps its
rows with `plantId = NULL`. App-private cropped photos (`files/photos/`) are
deleted only when no other plant, plant photo or Doctor history row uses the
same URI.

The local ERD is Firebase User 1—N Plant through external `ownerUid`, and
Plant 1—N CareLog through `plantId`. Firebase User is not a Room table.
Local `content://` image URIs are valid in Room and require no Firebase
Storage; this does not change `Listing.imageUrls` or `Post.imageUrl`.

## IDs between screens

`listingId` is the single shared identifier for Home, Listing Detail,
Community plant-sharing posts, and Conversations. Use stable `plantId`, `postId` and `conversationId` for destination reloads.
The current single listing draft is keyed by authenticated `ownerUid`; there
is no separate stored `draftId` to invent or pass. The user profile document ID is always the Firebase Auth UID.
Saved Listings store only a reference at `users/{uid}/savedListings/{listingId}`;
they do not duplicate the listing document.

Listing Detail contact opens or creates the deterministic Conversation from
the listing owner/current requester pair (retaining listing context), then navigates with
`conversationId` only. An existing Conversation screen also receives only
`conversationId` and reloads its data.

## Firestore Community contract (frozen)

Community Posts are authenticated plant-sharing content for questions, tips,
plant progress, and optional sharing of an existing Listing. They are not a
marketplace replacement or anonymous social feed. Physical paths are
`posts/{postId}`, `posts/{postId}/likes/{uid}`, and
`posts/{postId}/comments/{commentId}`.

The Post document contains exactly six fields: `authorId`, `text`, `category`,
`listingId`, `imageUrl`, and `createdAt`. Categories are `GENERAL`, `SHARING`,
`QUESTION`, `PLANT_WIN`, and `TIP`. `listingId` is a required String using
`""` when no Listing is attached; a nonblank reference requires `SHARING`, an
existing `ACTIVE` Listing at creation, and does not require the Post author
to own the Listing. No Listing fields are copied. `imageUrl` is required: `""`
when the post has no image, otherwise an HTTP(S) URL or `pmimg://<id>`
reference (max 2048 characters). Local content/file URIs are not remote post
fields. `postId` is the document ID only. Admin may edit text/category/image;
admin or author may delete the post. No automatic Firestore cascade exists.
Admin deletion removes comments/replies/likes in bounded batches before parent
and audit; account-deletion child cleanup is separately tracked in #49.

Likes use `posts/{postId}/likes/{uid}` with exactly one `createdAt` field. The
UID is the document ID, not a duplicated field. Members create their own Like
once and delete it to unlike; state and counts are derived, with no
`likeCount` on Post. Comments use the nested comments path with exactly
`authorId`, `replyToCommentId`, `text`, and `createdAt`. `replyToCommentId`
is `""` for top-level Comments or the ID of an existing top-level parent for
a reply, so only one reply level is allowed. Comment text is trimmed,
nonblank, and at most 1000 characters. Comment update and soft-delete fields are unsupported; delete is allowed to
admin, comment author or post author; comment count includes replies and is derived.

Community reads require authentication. The feed sorts by `createdAt`
descending and pages 20 posts at a time; the `GENERAL/SHARING/QUESTION/PLANT_WIN/TIP`
filters run in the query, and search covers the loaded text and resolved
author names. Missing authors
use “PlantMate member”; missing Listing references use “Plant listing
unavailable” without failing the whole Post. ERD relationships are USER 1—N
POST, POST 1—N COMMENT, USER 1—N COMMENT, USER N—N POST through POST_LIKE,
and POST N—0..1 LISTING, with a one-level Comment parent/reply relation.

Historical implementation stages were 6A-1 (feed/creation/likes) and 6A-2
(detail/comments/replies/listing context). Current images use Firestore media
references; Storage unavailability no longer implies text-only posts.

## Plant Doctor and Doctor History contract

Doctor is a private authenticated flow for selecting a plant image, requesting
an analysis from an approved provider, viewing diagnosis and guidance, and
optionally linking an existing local Plant. It is not a Community Post,
Listing field, CareLog replacement, or general AI chat. A diagnosis is never
fabricated: DoctorHistory is persisted only after a real provider result is
available; provider-unavailable and analysis-error states do not create rows.

The original v2 `doctor_history` table had these eight persisted fields:
`id` (auto-generated Long primary key), `ownerUid` (required Firebase Auth
UID), nullable `plantId` (optional `plants.id` reference), `imageUri`
(required persistent local `content://` URI), `diagnosis` (trimmed, nonblank,
maximum 200), nullable `confidence` (only a meaningful normalized 0.0..1.0
value), `guidance` (trimmed, nonblank, maximum 2000), and `createdAt` (epoch
milliseconds). There are no status, raw-response, prompt, token, provider
secret, or chain-of-thought fields.

`doctor_history.plantId` references `plants.id` with `ON DELETE SET NULL`
and an index. When non-null, the repository must verify that the linked Plant
belongs to the same authenticated `ownerUid`. All history reads are scoped to
the current UID; logout retains rows while another account cannot see them.
History reads are ordered by `createdAt DESC`; the current DAO can update
feedback and removes owner rows during account cleanup. The v10 twelve-column
schema is listed above.

Doctor uses a non-Room `PlantDiagnosisResult` and `GeminiDoctorAnalyzer` behind
`DoctorRepository`. The flow is
Doctor UI → ViewModel → Repository → Room DAO, Plant DAO, Auth, and analyzer;
UI never calls a provider or DAO directly. ViewModel states are IDLE, READY,
ANALYZING, QUALITY_WARNING, RESULT, and ERROR (an unconfigured or busy provider
is an ERROR with its own message). Confidence is hidden when
null, and copy should use qualified wording such as “Possible issue” and
"Suggested care". The current provider is Firebase AI Logic, as documented
below; the older backend-only policy remains a team reconciliation item.

The Doctor root remains the existing five-tab `DoctorFragment`. Planned
secondary screens are `DoctorResultActivity` and `DoctorHistoryActivity`,
receiving only a current result or stable `historyId`. Image selection uses a
readable, persistable local URI without broad storage permissions; provider
upload, if ever needed, is transient and does not make the URI remote data.

Historical Step 8A-1 added v1→v2 Doctor history while preserving plants/care
logs. The provider-unavailable stage ended when the documented Firebase AI
Logic integration was added; v10 is current. This reconciliation does not
approve a new provider or expose any secret.

**Update 2026-09-26 (approved):** the provider is Firebase AI Logic
(`GeminiDoctorAnalyzer`, validated by `DoctorAiParser`). the schema reached version 3:
`MIGRATION_2_3` adds nullable `plantName` (≤ 100), `scientificName` (≤ 120)
and `healthy` (Boolean) to `doctor_history`; older rows keep nulls. Photos
taken in the app live in app-private `filesDir/doctor_photos/` behind a
FileProvider. No Firestore data is added. The app language (English or
Vietnamese) is an AppCompat per-app locale, not account data. See `DOCTOR_AI_AND_LANGUAGE.md`.

**Update 2026-09-29 (approved):** the schema reached version 5 (historical). `MIGRATION_3_4` adds
`plants.wateringTimes`; `MIGRATION_4_5` adds nullable `doctor_history.feedback`
(the member's "was this right?" answer). Plant Doctor now sends the owner's own
records with the photo (the linked plant, its watering plan, the last six care
entries and a symptom the member tapped), checks the photo on the device for
darkness and blur before spending a request, and asks "was this right?" on the
result screen. Answers go to Firestore `doctorFeedback` (admin-only reads); the
photo goes to `doctorFeedbackPhotos` only when the member ticks the consent box.

ERD relationships are Firebase USER 1—N DoctorHistory via `ownerUid`, PLANT
1—0..N DoctorHistory via nullable `plantId`, and DoctorHistory belongs to one
authenticated local owner. Firebase User is not a Room entity.

## Firestore Safety contract (frozen)

PlantMate Safety lets authenticated members submit private reports, block and unblock another member, filter that member's shared content in their own app, and prevent new messaging interactions where a block exists in either direction. It is not itself an admin moderation dashboard, account-ban mechanism, content deletion flow, public report history, or follow/privacy system. Admin moderation is a separate contract in `ADMIN_FLOW_DESIGN.md`.

### Reports

Reports use reports/{reportId} with an auto-generated Firestore document ID; reportId is not a field. The document has exactly seven fields: reporterId, targetType, targetId, parentId, reason, details, and createdAt. reporterId is the authenticated creator. targetType is USER, LISTING, POST, or COMMENT. targetId is the nonblank target document ID. parentId is required and is empty for USER/LISTING/POST; for COMMENT it is the parent Post ID. reason is HARASSMENT, SPAM, SCAM, INAPPROPRIATE, or OTHER. details is trimmed, blank normalized to empty except OTHER requires nonblank details, and has a 500-character maximum. createdAt is a server timestamp equal to request.time on create.

Target existence is checked against the canonical target path. A member cannot report themselves, their own Listing, their own Post, or their own Comment. For COMMENT, both the parent Post and nested Comment must exist and the Comment author must not be the reporter. No MESSAGE or CONVERSATION target is supported; report a peer User from a Conversation. Ordinary client read, update, and delete are denied; only allowlisted admins may read reports (see `ADMIN_FLOW_DESIGN.md`). There is no report status, moderation note, deterministic ID, or automatic moderation side effect. The UI prevents rapid duplicate submission for one interaction; report deduplication is not part of the contract.

### Blocked users

The directional block relation is physically stored at users/{blockerUid}/blockedUsers/{blockedUid}. A block document has exactly one field, createdAt (server timestamp equal to request.time on create). blockedUid is the document ID only. Self-block is denied and the target User must exist. The blocker alone may read/list, create, or delete their own block documents. Update is denied. Blocking A→B does not create B→A. Unblock deletes only the current user's block reference. Blocks are not fields on the User profile and are not stored in Room or SharedPreferences.

When A has blocked B, A's app filters B-owned Listings in Home and Saved Listings, B-authored Posts, and B-authored Comments/replies, and guards Listing Detail for B-owned content where practical. Filtering uses the current member's blocked UID set through SafetyRepository; adapters do not read Firestore. Shared documents are not mutated and their Firestore read rules remain authenticated-community reads. This is one-way blocker-side feed filtering: A blocking B does not guarantee that B stops seeing A's shared content because B's private block list is not exposed and shared reads remain community-readable. Reviews remain visible and unchanged; Likes and aggregate counts are not modified. Blocking does not affect the blocker's own content, Garden, or Doctor history.

Messaging has bilateral enforcement. Conversation creation is denied if either users/{ownerId}/blockedUsers/{requesterId} or users/{requesterId}/blockedUsers/{ownerId} exists. Message creation derives both participants from the parent Conversation and is denied if either directional block exists. Firestore Rules enforce these checks without making either private block list readable by the other member. Existing Conversation and Message history remains readable to participants after a block; no history is deleted. New messages are denied while either block exists and may resume after all applicable blocks are removed. UI may show a neutral messaging-unavailable state and must not reveal that the peer blocked the current user.

### Safety rules contract and relationships

Current reports rules deny ordinary-member read and client update/delete, permit admin read, and allow authenticated create only with the exact seven fields, current-user reporterId, valid enum values and lengths, createdAt == request.time, existing-target validation, and self-report rejection. Current blockedUsers rules allow owner-only read/create/delete, require the target User to exist and createdAt == request.time, reject self-block, and deny update. Conversation create and Message create rules add both-direction block-existence checks while preserving all frozen Conversation/Message validation. Message reads remain participant-only and are not denied due to a block. Listing/Post/Comment read rules are not rewritten to filter blocked authors; mixed Firestore queries remain authenticated community reads, with blocker-side filtering in the client.

No report submission automatically blocks a member, removes content, changes Listing status, or disables an account. No Review is deleted or hidden by blocking. No report/message notification, report history, moderation state, or block count is added.

Conceptual ERD: USER 1—N REPORT as reporter; REPORT N—1 polymorphic target (USER/LISTING/POST/COMMENT); and USER N—N USER through directional BLOCKED_USER (blocker→blocked). Physical block data is a nested Firestore subcollection, not a Room entity or User field.

**Update 2026-09-27 (approved): profile avatars.** `users/{uid}.avatarUrl` now holds `""`
(initials), `preset:<key>` (a built-in plant avatar, `util/AvatarBinder.PRESETS`) or
`pmimg://<id>` (a photo in `images/{id}` owned by the member). Members may change it from
Edit profile; the rules' `validAvatar()` checks the format and, for photos, that the image
document belongs to the caller. Admin edits still leave the avatar unchanged. Avatars show on
Profile, public member profiles, posts, post detail and listing owner rows.
