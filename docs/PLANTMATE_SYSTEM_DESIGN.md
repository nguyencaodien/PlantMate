# PlantMate System Design

**Status: APPROVED / FROZEN FOR MVP**

PlantMate is a local plant-sharing community. Its priorities are Give Away, Trade, Plant Community, Plant Care, and exchange discussion. Selling is supported as a secondary listing type; the MVP has no payment, shipping, or cart.

## 1. Product definition

PlantMate lets authenticated members share plants, discuss exchanges, maintain a personal garden, and participate in a plant community. The app is a Java/XML Android application under `com.example.ungdungchiasecaycanh`.

## 2. Goals

- Support Give Away, Trade, and Sell listings with one shared `listingId`.
- Provide personal garden and care-log features backed by local storage.
- Provide community posts, comments, and listing-linked discussions.
- Support text conversations between members about listings.
- Demonstrate a clear Java/XML Android architecture suitable for the course.

## 3. Non-goals

- Payment, shipping, cart, follower system, video, livestream, group chat, or voice/video calls.
- Advanced recommendation, full offline replication, and complex moderation.
- Client-side AI secrets. (AI plant identification and diagnosis is approved through Firebase AI Logic; see `DOCTOR_AI_AND_LANGUAGE.md`.)

## 4. Actors

- Guest: signs in, registers, or requests a password reset.
- Member: browses listings, community content, and conversations.
- Listing owner: creates and manages own listings and exchange status.
- Garden owner: manages personal plants, care logs, and reminders.
- Community author: creates posts and comments.

## 5. Functional modules

- Authentication and profile
- Listings: Give Away, Trade, Sell, saved listings, and My Listings
- Inbox: one-to-one, Listing-linked TEXT conversations between a Listing
  owner and an authenticated requester
- Garden: plants, care logs, and care reminders
- Community: posts, likes, comments, and listing sharing
- Inbox: listing-linked text conversations
- Doctor: basic care guidance plus AI photo identification and diagnosis (Firebase AI Logic); deeper analysis is Phase 2
- Language: English and Vietnamese, chosen in Settings
- Settings

## 6. System architecture

```text
Activities / Fragments / XML / RecyclerView
                ↓
       ViewModel and LiveData
                ↓
            Repository
                ↓
 Room / Firebase / SharedPreferences
```

The application remains Java 11 with XML layouts, Material components, RecyclerView, Room, Firebase, and no dependency-injection or multi-module architecture.

## Listing conversations and messages

Messaging is Listing-context first. Conversations live at
`conversations/{conversationId}` and messages at
`conversations/{conversationId}/messages/{messageId}`. A Listing and its
first requester use the deterministic Conversation ID
`{listingId}__{requesterId}`, so repeated contact reuses one Conversation.
The requester must be authenticated and different from the Listing owner.
Each Conversation has exactly two participants: the immutable Listing owner
and requester.

The Conversation has exactly seven fields: `listingId`, `ownerId`,
`requesterId`, `participantIds`, `lastMessage`, `createdAt`, and `updatedAt`.
It stores references and the latest-message summary only, never copied
Listing/profile content or a message array. It may exist with an empty
`lastMessage` before the first message.

Each Message has exactly four fields: `senderId`, `type`, `text`, and
`createdAt`. MVP writes accept only `TEXT`; client text is trimmed, nonblank,
and at most 1000 characters. Message and Conversation creation/update times
use server timestamps. Messages cannot be edited or deleted by the client;
Conversation deletion is denied. Participants alone may read the Conversation
and its messages. Only the requester may create the Conversation after the
referenced Listing owner is verified. Conversation participants and Listing
context remain immutable; only `lastMessage` and `updatedAt` can change.

Inbox queries by `participantIds array-contains` the authenticated UID and
sorts by `updatedAt` descending. It remains a secondary Activity and does not
alter the five-tab primary navigation. Inbox and Conversation screens resolve
peer and Listing context through canonical User and Listing records instead
of duplicating those values in messaging documents.

The ERD may express USER N:N CONVERSATION using a conceptual
`CONVERSATION_PARTICIPANT` associative entity. Firestore stores the two UIDs
in `participantIds` and has no participant subcollection. A future Review
may use a completed REHOMED Listing and its Conversation as interaction
context; Review itself remains deferred. Read receipts,
typing indicators, notifications, and trade-offer messages are also deferred.

## 7. Local vs remote responsibility

| Data | Source of truth |
|---|---|
| Authentication/session | Firebase Authentication |
| Users, listings, saved references, posts, likes, comments, conversations, messages | Cloud Firestore |
| Optional published shared images | Future supported remote image service; unavailable in the current MVP configuration |
| Personal plants, care logs, listing drafts | Room |
| Settings | UID-scoped SharedPreferences / PreferenceFragmentCompat |

Room remains account-scoped local data. Firestore remains shared multi-user data; do not mirror Firestore into Room for the MVP.

The canonical Firestore user profile is `users/{uid}`, using the Firebase
Authentication UID as its document ID. Its exactly six MVP fields are
`displayName`, `locationText`, `avatarUrl`, `bio`, `createdAt`, and
`updatedAt`; all are required. `locationText` is manually entered coarse
area text (1–100 characters), while `avatarUrl` and `bio` start empty and
the time fields use server timestamps. The document is public-in-app data for
authenticated members. Only the owner may create or update it,
`createdAt` is immutable, client deletion is unsupported, and no email,
password, token, exact location, preferences, roles, or derived statistics
are stored there. Firestore create and update rules preserve the exact
six-field schema and enforce the canonical 500-character bio maximum.

Listings keep the exact twelve-field Firestore schema. The required
`imageUrls` field is a list of zero to five legitimate remote image URLs.
An empty list is the normal current-MVP publication value and selects the
local PlantMate placeholder in Listing UI. Firebase Storage remains
unavailable, so remote image upload is deferred and is not a prerequisite for
publishing a Listing. Local `content://` or `file://` locations, base64 data,
fake URLs, and placeholder URLs are never written into `imageUrls`.

## 8. Main navigation architecture

`MainActivity` contains exactly five primary Fragments, in this order: **Home, Garden, Doctor, Community, Profile**. Inbox, Settings, and AI are contextual secondary Activities. Stable IDs travel in Intent extras; Activities reload data through their ViewModels.

## 9. MVP boundary

MVP includes authentication, five-tab navigation, listings, Garden with care logs and reminders, Community posts/likes/comments, Inbox with text, photo, and voice messages, Profile, Settings, Room, Firebase, and a basic Doctor flow. AI image diagnosis, advanced trade offers, reposts, advanced sync, and advanced Doctor analysis are Phase 2.

## 10. Technical constraints

- Namespace/applicationId: `com.example.ungdungchiasecaycanh`.
- Java and XML only; do not introduce Kotlin, Compose, Flutter, or React Native.
- minSdk 29, targetSdk 37, compileSdk 37, Java 11.
- Main navigation uses `MainActivity` plus the five frozen Fragments; other screens are Activities.
- AI requests must go through a secure backend or Cloud Function. Never include an AI API secret in the Android client.

## 11. Implementation principles

- Keep a single canonical listing record and reuse its `listingId` everywhere.
- Use stable IDs, Intent extras, ViewModels, and repositories; avoid passing large domain objects between Activities.
- Use resources for UI strings, colors, and dimensions.
- Each data screen must plan for loading, content, empty, error/retry, and relevant offline or permission states.
- Preserve ownership boundaries: shared writes require Firebase authorization; local personal data is scoped to the active account.
- Registration collects display name, email, password, and coarse manual location; it does not collect confirm password. Registration requires an eight-character minimum password; sign-in requires only a non-empty password. After Auth account creation, the profile is created at `users/{FirebaseAuth UID}`, and navigation waits for both operations. Profile-creation failure triggers best-effort Auth deletion and sign-out. Username is Phase 2 / TBD.
- Profile rules require authenticated reads, owner-only create/update, immutable `createdAt`, server-timestamp `updatedAt`, string `locationText` of 1–100 characters, `bio` of at most 500 characters, and exactly the six approved fields.
- Follow [NAVIGATION.md](NAVIGATION.md), [DATA_MODEL.md](DATA_MODEL.md), [ANDROID_STRUCTURE.md](ANDROID_STRUCTURE.md), and [MVP_SCOPE.md](MVP_SCOPE.md) for the frozen details.

## 12. Garden and local Room contract

Garden is a private, account-scoped local Room feature rooted at
GardenFragment. PlantMateDatabase is the local source of truth for PlantEntity
in plants and CareLogEntity in care_logs; Garden records are never written to
Firestore or Storage in MVP. The active Firebase Auth UID is stored as
immutable PlantEntity.ownerUid and every user-facing query filters by that UID.
Logout keeps rows on the device while making Garden unavailable until an
authenticated session exists; another account sees only its own rows.

PlantEntity has exactly eleven fields: auto-generated id, ownerUid, nickname,
species, local-only imageUri, reminderEnabled, wateringIntervalDays, nullable
epoch-millisecond nextWateringAt, createdAt, updatedAt, and wateringTimes (daily
reminder times; empty for every-N-days plans). There is no Plant
status or diagnosis field. Disabled reminders normalize to false/0/null;
enabled reminders require an interval from 1–365 and a non-null target.

CareLogEntity has exactly five fields: auto-generated id, plantId, enum String
type, note, and epoch-millisecond createdAt. CareType is WATER, FERTILIZE,
REPOT, PRUNE, or NOTE; notes are capped at 500 characters and NOTE requires
non-blank text. Ownership is derived through the parent Plant, not duplicated
on CareLog. A Plant has many CareLogs and the planned Room foreign key uses
cascade delete. A successful WATER event may advance nextWateringAt.

Implementation phases are 7A-1 Room/database, DAOs, repository, Plant CRUD,
Garden list, and Plant Detail; 7A-2 CareLog actions/history and watering state
updates; and 7A-3 local reminder scheduling, receiver, notification, and
reschedule/cancel behavior. Exact alarms are not required by the contract.

## 13. Profile editing and Settings contract

Profile remains the fifth bottom-navigation destination and displays the
canonical User's `displayName`, coarse `locationText`, optional nonblank
`bio`, and supported avatar or local placeholder. It does not invent rating,
follower, or exchange statistics. Profile links to Inbox, My Listings, Saved
Plants, My Reviews, Edit Profile, and Settings.

Edit Profile loads the current profile through Profile/Edit ViewModel →
`UserRepository` → `FirebaseUserDataSource`; it takes no UID from UI input or
Intent. The only editable values are `displayName`, `locationText`, and
`bio`. Display name keeps the existing registration validation: trim and
nonblank, with no maximum currently defined in the validator or User model.
Location is trimmed, nonblank manual coarse text up to 100 characters. Bio
is trimmed, may be empty, and is limited to 500 characters. A profile update
changes only these values and `updatedAt` using a server timestamp; it
preserves `avatarUrl` and immutable `createdAt`, updates the existing
Firestore document, and never changes Firebase Auth email. Avatar upload and
email/password editing remain unsupported. Failure retains form input and
allows retry; success returns to Profile for canonical reload.

Settings is a contextual secondary Activity with `PreferenceFragmentCompat`.
Its only functional stored preference in current MVP is
`care_notifications_enabled` (Boolean, default `true`) in SharedPreferences
scoped by authenticated Firebase UID. Logout preserves values; another
account uses its own namespace. Settings are not Room or Firestore data and
do not change the six-field User schema.

The care-notification preference is separate from each Plant's Room reminder
schedule. OFF preserves Plant reminder fields and Garden due/upcoming state,
while canceling/suppressing the current account's OS alarms where practical;
the receiver also checks current UID and preference before delivery. ON
reconciles future valid schedules without immediately notifying for overdue
Plants. Android notification permission and channel state remain separate.
Notification settings may open system app/channel settings; About and app
version are informational rows, not persisted values. Do not add inactive
community/message notification, theme, sync, or backup switches. No Room
schema change is needed; the existing database remains version 2.

## Reference Notes

`docs/reference/user-handoff.txt` names `com.example.plantmate`; the approved namespace/applicationId is `com.example.ungdungchiasecaycanh`. The reference React prototype is visual and interaction evidence only and does not alter this contract.

## Community contract

Community remains the fourth of the five primary tabs and is rooted at
`CommunityFragment`. Posts are authenticated plant discussions, questions,
tips, plant wins, and optional Listing shares, not an anonymous social feed
or marketplace replacement. The Post path is `posts/{postId}` with exactly
`authorId`, `text`, `category`, `listingId`, `imageUrl`, and `createdAt`.
Categories are `GENERAL`, `SHARING`, `QUESTION`, `PLANT_WIN`, and `TIP`.
`listingId` uses `""` when absent; a nonblank reference requires `SHARING`
and an existing `ACTIVE` Listing at creation, without copying Listing fields
or requiring the Post author to own it. `imageUrl` remains `""` while Storage
is unavailable. Posts are immutable in MVP.

Likes are UID-keyed nested documents with only server `createdAt`; counts and
liked state are derived. Comments are immutable nested four-field documents;
`replyToCommentId == ""` marks a top-level Comment, and a top-level parent
permits exactly one reply level. The architecture is Community UI →
CommunityViewModel → CommunityRepository → FirebaseCommunityDataSource.
The feed uses authenticated Posts sorted newest-first with local category
filters/search and canonical User/Listing enrichment. CreatePostActivity
receives optional `listingId` only and PostDetailActivity receives `postId`
only. Implementation is split into 6A-1 for feed, creation, Likes, and
Post/Like rules, and 6A-2 for detail, Comments, replies, sharing, and Comment
rules.

## Rating and Review contract

Reviews are directed member-to-member trust records after a Listing interaction, not plant or product ratings. They are stored at `reviews/{reviewId}`, with deterministic ID `{listingId}__{reviewerId}__{revieweeId}` and no persisted reviewId field. Exactly six fields are allowed: `listingId`, `reviewerId`, `revieweeId`, `rating`, `comment`, and `createdAt`. Rating is an integer 1–5; comment is normalized to empty when blank and capped at 500 characters; `createdAt` is an immutable server timestamp. Create and authenticated read are supported; update and delete are denied.

Eligibility requires an authenticated reviewer, distinct reviewer and reviewee, a `REHOMED` Listing, the Listing owner as one of the two members, and an existing deterministic owner/requester Conversation matching that Listing. Both directions are allowed once per Listing/member direction. The preferred entry point is Conversation; arbitrary Listing visitors cannot create Reviews. User aggregates are derived from Reviews and are not stored as User fields.

The MVP Listing schema has no accepted-exchange-partner field. If multiple requesters contacted a Listing before it became `REHOMED`, Conversation existence cannot identify the completed partner. A future Exchange entity or accepted participant record is needed for stricter attribution; the Listing schema remains unchanged.

## Plant Doctor and Doctor History contract

Doctor is a private authenticated image-analysis flow rooted at `DoctorFragment`, with optional local Plant linking and local history. It is not a Community Post, Listing field, CareLog replacement, or general chat. Diagnosis, confidence, and guidance are never fabricated; unavailable providers save no history.

The planned Room table `doctor_history` has exactly eight fields: generated `id`, `ownerUid`, nullable `plantId`, persistent local `imageUri`, normalized `diagnosis`, nullable normalized `confidence` in 0.0..1.0, normalized `guidance`, and epoch-millisecond `createdAt`. Reads are owner-scoped and newest-first; rows are immutable in MVP. A repository verifies linked Plant ownership.

Doctor follows UI → ViewModel → Repository → Room/Auth/Plant data and `GeminiDoctorAnalyzer` (Firebase AI Logic). No API key or provider secret belongs in Android; a future analyzer uses a trusted backend. Step 8A-1 adds the explicit Room v1→v2 migration and local result/history foundation; Step 8A-2 connects an approved provider. No Doctor Firestore collection or schema change is added.

## 14. Safety: Report and Block contract

Safety is an authenticated member feature for reporting inappropriate or suspicious users/content, blocking or unblocking another member, filtering that member's content from the current member's app, and preventing new messaging when either participant has blocked the other. It is not an admin moderation console, automatic ban/deletion system, public report history, or social follow system.

Reports are private write-only client records at reports/{reportId} with an auto-generated ID and exactly seven fields: reporterId, targetType, targetId, parentId, reason, details, and createdAt. Target types are USER, LISTING, POST, COMMENT; reasons are HARASSMENT, SPAM, SCAM, INAPPROPRIATE, OTHER. The reporter is the authenticated UID. parentId is empty except for a Comment report, where it is the parent Post ID. Target existence and self-report checks are required. Details are trimmed, capped at 500 characters, blank as empty except OTHER requires nonblank details. The timestamp is server-controlled. Ordinary client read, update, and delete are denied. A report does not itself block, delete, hide, or change the target.

Blocks are directional references at users/{blockerUid}/blockedUsers/{blockedUid} with only server createdAt. The document ID identifies the blocked member. Self-block is denied, the target User must exist, and only the blocker reads/creates/deletes their own entries; updates are denied. Blocks remain private and are neither User fields nor Room or preference data.

For a member's own view, the app filters content owned/authored by UIDs in their block set from Home Listings, Saved Listings, Listing Detail, Community Posts, and Comments/replies where practical. A Safety repository supplies the block set; adapters do not read Firestore. This filtering does not mutate shared content or change Listing/Post/Comment community-read rules. It is deliberately one-way: if A blocks B, A's app hides B's relevant content, but the MVP cannot guarantee B stops seeing A's shared content because B's block list is private and the shared feed remains readable to authenticated members.

Messaging is bilateral at the write boundary. Firestore Rules deny new Conversation creation and new Message creation if a block exists in either direction between the Listing owner and requester/current participants. Existing participants can continue reading existing Conversation and Message history after a block; history is not deleted. A current member who blocked the peer can see a messaging-unavailable state. If the peer blocked the current member, a denied write uses neutral copy and does not disclose the private block direction. Listing Detail contact is unavailable for a member the current user personally blocked; a reverse block is surfaced as a safe generic failure.

Safety entry points stay contextual: Listing Detail can report the Listing or block its owner; Post Detail can report the Post or block its author; Comments/replies can report the Comment or block its author; Conversation can report or block the peer User. Reports from Conversation target USER, never a Message. A Blocked Users secondary screen under Profile/Settings resolves current profile names as display-only enrichment and supports unblock. Inbox may retain blocked conversations and indicate that messaging is unavailable. Reports do not hide Reviews, alter Likes/counts, or affect a member's own Listings, Posts, Garden, or Doctor history.

Future rules preserve current User, Listing, Post, Comment, Conversation, Message, and Review contracts. Report create validates the exact schema, target existence, ownership/self-report, enums, limits, and server time; ordinary reads and mutations are denied. Block subcollection rules are private to its owner. Conversation and Message create rules check both directional block documents. Existing participant message reads remain allowed after a block. Listing/Post/Comment read rules are not filtered by block because list-query authorization over mixed authors would be brittle; the app applies current-blocker filtering. User remains six fields and Room remains version 2.
