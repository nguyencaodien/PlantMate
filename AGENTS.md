# PlantMate Coding Instructions

## Project contract

- PlantMate is a local plant-sharing community: Give Away, Trade, community, care, and exchange discussion come before selling.
- The approved design contract lives in `docs/`. Read `docs/PLANTMATE_SYSTEM_DESIGN.md` and the focused docs before changing a feature.
- Do not modify `docs/reference/**`; it is preserved reference material only.

## Android stack

- Keep application code in Java and layouts in XML.
- Use the actual namespace and applicationId: `com.example.ungdungchiasecaycanh`.
- Keep Java 11 and the existing Android SDK configuration unless a task explicitly changes them.
- Use Material components and RecyclerView where a list is required.
- Do not introduce Kotlin, Jetpack Compose, Flutter, React Native, a DI framework, or multi-module Clean Architecture without explicit approval.

## Architecture

- Follow `UI → ViewModel → Repository → Data`.
- UI consists of Activities, Fragments, XML layouts, and RecyclerView adapters.
- Keep ViewModels near their feature UI packages and expose observable UI state with LiveData where appropriate.
- Repositories own data access; do not put Firebase or Room access directly in Activities or Fragments.
- Use Room for account-scoped personal data and drafts. Use Firebase Authentication for session, Firestore for shared data, and Storage for shared images.
- Do not mirror Firestore data into Room for MVP unless the design contract is explicitly updated.

## Navigation

- `MainActivity` has exactly five primary Fragments: `HomeFragment`, `GardenFragment`, `DoctorFragment`, `CommunityFragment`, and `ProfileFragment`.
- Bottom-navigation labels and order are exactly: Home, Garden, Doctor, Community, Profile.
- Inbox, Settings, and AI remain contextual secondary Activities.
- Pass stable IDs through Intent extras, such as listing, plant, post, conversation, and draft IDs. Reload data through the destination ViewModel; do not pass large models or bitmaps.

## Data and security

- Preserve one shared `listingId` across Home, listing details, community, and conversations.
- Keep Room queries and local data account-scoped by the authenticated user ID.
- Never hard-code AI API secrets. Route AI work through a secure backend or Cloud Function.
- Apply Firebase authorization and ownership checks as part of each remote feature.

## Feature behavior

- Use a stable ID when one feature opens another: listing, plant, post, conversation, and draft screens each reload their own data.
- Treat Room as the source of truth for Garden, care logs, and listing drafts.
- Treat Firestore as the source of truth for shared listings, community content, saved references, and conversations.
- Keep one shared listing record. Community posts and conversations may reference it but must not maintain conflicting listing state.
- Keep selling secondary to Give Away and Trade. Do not introduce payment, cart, shipping, or delivery flows into MVP work.
- Keep advanced AI, advanced Doctor analysis, image messages, and advanced trade offers out of MVP unless a task explicitly expands scope.

## UI and state quality

- Keep UI work on the main thread and execute database or network work away from it.
- Represent the appropriate loading, content, empty, and error/retry state for each data-backed screen.
- Respect notification and other runtime permissions; the associated core feature must still have a useful state when permission is denied.
- Keep authentication changes from exposing one account's Room data, cached UI, reminders, or callbacks to another account.
- Use `AlarmManager`, receivers, notifications, and schedulers only for their defined feature purpose.
- Preserve Android lifecycle behavior through ViewModels; do not use Activity or Fragment fields as durable feature state.

## Files and resources

- Follow the frozen package and resource structure in `docs/ANDROID_STRUCTURE.md`.
- Use `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`, and `bottom_sheet_*.xml` layout names.
- Put user-visible strings, colors, and dimensions in resources when appropriate.
- Reuse a RecyclerView adapter only when its item layout and behavior match.

## Navigation ownership

- Keep primary destinations inside `MainActivity`; use secondary Activities for detail, creation, settings, and contextual flows.
- Do not add a sixth bottom-navigation item for Inbox, Settings, Sync Center, or AI.
- Keep the Back stack predictable. Do not duplicate primary destinations when switching tabs.
- After a successful publish operation, do not leave a reusable creation form in the Back stack.
- Sign-out must remove authenticated navigation history before opening the sign-in screen.

## Testing expectations

- Add or update tests when feature logic changes and an appropriate unit or instrumentation test is available.
- For Room changes, verify account ownership, entity relationships, and CRUD behavior.
- For Firebase changes, verify owner/member access and error handling where the environment supports it.
- For navigation changes, verify the primary tab order, contextual Up behavior, and ID handoff.
- Keep tests in Java unless an explicit task changes the project language.

## Documentation contract

- Keep `docs/PLANTMATE_SYSTEM_DESIGN.md` as the approved MVP overview.
- Use `docs/NAVIGATION.md` for screen ownership and navigation changes.
- Use `DESIGN.md` (project root) for colours, typography, spacing, shapes and component rules; check every UI change in light and dark theme.
- Use `docs/DATA_MODEL.md` for source-of-truth and ID decisions.
- Use `docs/ANDROID_STRUCTURE.md` for package, UI, adapter, and resource conventions.
- Use `docs/MVP_SCOPE.md` to decide whether a requested capability belongs to MVP or Phase 2.
- Update a canonical design document when an approved implementation decision changes its contract.

## Change discipline

- Make only task-relevant changes; avoid unrelated refactors, formatting sweeps, or dependency changes.
- Validate changed code in proportion to the task and report what was checked. Do not claim a build or test passed unless it was run successfully.
- Read the existing feature code and its linked design document before making a structural change.
- Preserve unrelated user changes in a dirty working tree.
- When a task changes Android code, inspect the targeted diff and run the smallest relevant verification available.
- The project is inside a dirty parent Git repository. Never touch parent-repository files, including `database/` or `procedures/`; never use broad Git cleanup/reset/restore/stash commands.
- Scope all work to the project root — the directory that contains `app/`, `gradlew`, and `settings.gradle.kts`. The project root may be located anywhere on the contributor's machine; do not hard-code an absolute path.
