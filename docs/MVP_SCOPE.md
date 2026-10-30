# MVP Scope

## MVP required

| Feature | Minimal accepted behavior |
|---|---|
| Authentication | Register with displayName, email, password (minimum 8 characters), and manually entered coarse location; sign in with valid email and non-empty password; reset password, sign out, and maintain an authenticated session. Registration has no confirm-password field and completes only after Auth plus Firestore profile creation. |
| Main navigation | `MainActivity` shows Home, Garden, Doctor, Community, and Profile. |
| Listings | Browse authenticated ACTIVE listings in RecyclerView with ALL/GIVE_AWAY/TRADE/SELL filters; create Give Away, Trade, or Sell listings with preview and publish using required `imageUrls = []` when remote upload is unavailable; open detail, save a reference, share to Community, and start a discussion. |
| Garden | Create/manage personal plants, view plant detail, add care logs, and water now. |
| Care reminders | Schedule local watering reminders from Room Plant data when the UID-scoped care-notification preference and Android notification permission/channel allow delivery. |
| Community | Create posts, open post detail, like, and comment. |
| Inbox | Open or reuse one deterministic, one-to-one Listing-linked Conversation per requester; participants exchange immutable TEXT messages up to 1000 characters. Inbox is secondary and participant-scoped. |
| Profile | Display canonical displayName, coarse locationText, optional bio, and supported avatar/placeholder; access Inbox, Saved Plants, My Listings, My Reviews, Edit Profile, and Settings. |
| Community | Browse authenticated plant Posts, filter/search the loaded feed, create text or Listing-reference Posts, Like/Unlike, open Post Detail, and support immutable Comments with one reply level. |
| Settings | PreferenceFragmentCompat with one functional setting: UID-scoped care_notifications_enabled (default true); system notification settings and app information actions. |
| Admin | One Console-allowlisted admin account (`admins/{uid}`) manages the app in-app: CRUD on listings, posts and comments, member profile edits and suspension, report review, and an append-only activity log. See `ADMIN_FLOW_DESIGN.md`. |
| Safety | Authenticated members can submit private reports, block/unblock members, filter blocked authors from their own shared-content views, and rely on bilateral block checks to prevent new messaging writes. |
| Doctor | Basic care guidance by symptom, plus an AI plant scan (take or choose a photo) that identifies the plant and suggests a diagnosis through Firebase AI Logic, with honest error states. See `DOCTOR_AI_AND_LANGUAGE.md`. |
| Language | Settings offers English and Vietnamese (or the device language); all UI strings and Mầm's answers are translated. |
| Data | Use Room for local Garden/drafts and Firebase for authentication and shared data. |

## Chat media (approved expansion)

- Inbox conversations may send one photo per message and voice messages of
  0.5–60 seconds, stored privately in Firestore `chatMedia` and readable only
  by the two participants. Voice recording needs the RECORD_AUDIO runtime
  permission; when it is denied, text and photo messages still work.
- Image editing, video, group chat, read receipts, and voice/video calls stay
  out of scope.

## Friends and green moments (approved expansion, 2026-10-09)

- Members send a friend request from another member's profile ("Add friend"); the other member
  accepts or declines it from the profile or from Profile › Friends, where friends can also be
  removed. Blocking a member also ends the friendship and any pending request.
- Community shows "Khoảnh khắc xanh" (green moments) as in the canvas: "New story", then the
  member's own stories and their friends' stories. A story is one 9:16 photo, a title (1–24
  characters, with suggestions) and an optional caption (up to 200 characters). Only the author
  and their friends can read it, and it is hidden 24 hours after posting. Authors can delete their
  own stories. Opened stories get a grey ring (account-scoped, in Room).
- Story replies, reactions, view lists, friend suggestions, contact import and friend-only posts
  stay out of scope.

## In-app notifications (added 2026-10-17)

- A bell on the Home header (beside Inbox and Saved) shows a red unread count and opens
  `NotificationsActivity`: today's watering (from Room), friend requests waiting, and community
  notifications when someone comments on your post, replies to your comment, mentions you with
  "@Name" or likes your post. Filters: All / Plant care / Community.
- No push for community notifications (Spark has no server): they appear when the member opens the
  app. Watering reminders keep their local AlarmManager notification.

## Phase 2 optional

- Advanced Plant Doctor analysis beyond one-photo identification and diagnosis
- Advanced trade-offer interactions
- Read receipts, typing indicators, and message notifications
- Rating/Review implementation (future eligibility may use a completed REHOMED Listing and its Conversation context)
- Repost and external community sharing
- Advanced sync recovery

The frozen Review contract remains deferred in the current implementation
scope. When implemented, Reviews will be immutable directed member Reviews
created only with `REHOMED` Listing and owner/requester Conversation evidence.
Aggregates will be derived from `reviews` rather than denormalized into
User, and the known ambiguity around multiple requesters will remain until a
future Exchange or accepted-participant model is introduced.

## Out of scope

- Payment gateway, cart, shipping, and delivery tracking (a MoMo/VNPay transaction-confirmation
  plan is under discussion; it needs a backend and cannot hold members' money)
- Identity verification through VNeID (needs an official connection to the national e-ID
  system); selling instead requires a verified email. Profile › "VNeID verification" is a
  clearly labelled demo of the intended flow: no identity data is sent or stored and no badge is
  given. Completing it sets an account-scoped local flag; creating a SELL listing and messaging
  the seller of a SELL listing ask for it first. This is a UI gate only (it cannot be enforced in
  Firestore rules without a real VNeID connection); the enforced check stays the verified email.

Community's "Post" button offers a discussion post or a give-away / trade / sale listing. The
listing is created with the same screens and the same shared record as Home, then shared as a
SHARING post, so both tabs show one listing without duplicated state.
- Follower system, video/reels, livestream, group chat, and voice/video calling
- Advanced recommendation engine
- Full offline-first replication
- Complex moderation dashboard

Community image upload remains deferred while Firebase Storage is unavailable.
Current Post creation writes `imageUrl == ""`; local URIs and fake URLs are
not remote Post data. Like and Comment counts remain derived. Content
edit/delete flows and deeper reply nesting remain outside the current MVP
implementation (in-app notifications were added on 2026-10-17, see above).

Listing publication does not depend on Firebase Storage. Its exact
twelve-field document retains required `imageUrls`, with zero to five
legitimate remote URLs. Current MVP publication writes an empty list and uses
the local PlantMate placeholder. Remote Listing image upload remains deferred;
local URIs, base64 data, fake URLs, and placeholder URLs are never persisted
as remote Listing images.

### Garden local-data boundary

Garden MVP is private, local-first Room data scoped by the active Firebase Auth
UID. plants stores exactly the eleven-field PlantEntity contract, including
immutable ownerUid, optional local content:// image URI, and normalized
watering reminder fields. care_logs stores exactly the five-field CareLogEntity
contract and derives ownership through its parent Plant. CareType is limited to
WATER, FERTILIZE, REPOT, PRUNE, and NOTE; NOTE requires text and notes are
capped at 500 characters. Garden data is retained across logout but is
inaccessible without authentication and is never written to Firestore.

Garden implementation is phased: 7A-1 Room foundation and Plant CRUD, 7A-2
CareLog actions/history and watering rescheduling data, and 7A-3 local
AlarmManager/notification scheduling. Search, Plant deletion, exact alarms,
Firebase Storage, and Doctor diagnosis fields are not immediate Garden
requirements.
Doctor local history is frozen for Step 8A-1: an explicit Room v1→v2
migration, image selection, provider abstraction, result/history UI, and
owner-scoped reads. The eight-field `doctor_history` row is written only after
a real provider result. Until a trusted backend is approved, Doctor shows an
unavailable state and saves no fabricated diagnosis, confidence, or guidance.
Advanced provider integration remains Step 8A-2/Phase 2; no Doctor Firestore
data, API keys, notifications, or Plant/CareLog/User schema fields are added.

**Update 2026-09-26 (approved):** Step 8A-2 is implemented with Firebase AI Logic
(Gemini, no key in the client). Room v3 adds nullable `plantName`,
`scientificName` and `healthy` to `doctor_history`. See `DOCTOR_AI_AND_LANGUAGE.md`.

### Profile and Settings boundary

The canonical `users/{uid}` document remains exactly six fields. Edit Profile
changes only trimmed `displayName`, manually entered coarse `locationText`,
and trimmed `bio`; displayName remains required/nonblank with no maximum
specified by current registration validation, location is required and at
most 100 characters, and bio may be empty and is at most 500 characters.
`avatarUrl` is preserved, `createdAt` is immutable, and `updatedAt` is a
server timestamp. Email/password editing and avatar upload are not part of
this flow.

Settings contains only the functional `care_notifications_enabled` Boolean
in current MVP. It is stored in SharedPreferences scoped by authenticated
UID, defaults to true, survives logout, and is not written to Firestore or
Room. It gates local watering notification delivery independently of each
Plant's reminder schedule and Android notification permission. Settings
contains no theme, community/message notification, sync, or backup switches.


### Safety boundary

Report and Block are authenticated Firestore features, not moderation tools. Reports use reports/{reportId} with an auto-generated document ID, exactly seven fields, valid USER/LISTING/POST/COMMENT targets, one of five canonical reasons, optional trimmed details up to 500 characters (required for OTHER), and server creation time. Reports are create-only for ordinary clients and cannot be read back, updated, or deleted. Submission does not block a member or change target content/status.

Blocks use the private directional path users/{uid}/blockedUsers/{blockedUid} with only createdAt. A member reads, creates, and deletes only their own block references; update and self-block are denied. The blocker-side app filters the blocked member's Listing, Post, and Comment content where practical. This does not guarantee reciprocal feed hiding. Messaging rules check both directions: a block prevents new Conversation and Message writes, while existing Conversation/Message history remains readable to participants. Reviews, Likes, and counts remain unchanged. Safety data adds no User fields, Room entity, database version change, or SharedPreferences value.
