---
name: PlantMate
description: Local plant-sharing community for Vietnamese cities — give, trade, care, ask Mầm. White, ink and one green; real plant photos as the hero (canvas B).
colors:
  background: "#FFFFFF"
  surface: "#FFFFFF"
  panel: "#F3F4F3"
  panel-line: "#E9ECE9"
  brand-surface: "#E6F2EA"
  rank-panel: "#CFE5D6"
  primary: "#1F7A45"
  on-primary: "#FFFFFF"
  primary-tint: "#E6F2EA"
  on-primary-tint: "#14532F"
  forest: "#1F7A45"
  ink: "#111412"
  text-primary: "#111412"
  text-secondary: "#5B625D"
  border: "#E4E7E4"
  nav-capsule: "#111412"
  input-border: "#CFCFCF"
  danger: "#B3261E"
  star: "#E0A100"
  status-reserved: "#D97706"
  dark-background: "#0E100F"
  dark-surface: "#171917"
  dark-panel: "#191C1A"
  dark-rank-panel: "#1E3A29"
  dark-primary: "#63C98E"
  dark-on-primary: "#0B1F12"
  dark-text-primary: "#EEF1EE"
  dark-text-secondary: "#A1A8A3"
  dark-nav-capsule: "#1F2320"
typography:
  hero:
    fontFamily: "Roboto, sans-serif"
    fontSize: "30sp"
    fontWeight: 700
    letterSpacing: "-0.02em"
  screen-title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "26sp"
    fontWeight: 700
    letterSpacing: "-0.02em"
  detail-title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "24sp"
    fontWeight: 700
    letterSpacing: "-0.015em"
  section-title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "19sp"
    fontWeight: 700
    letterSpacing: "-0.01em"
  card-title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "17sp"
    fontWeight: 500
    letterSpacing: "-0.01em"
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "15sp"
    fontWeight: 400
  label:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
  meta:
    fontFamily: "Roboto, sans-serif"
    fontSize: "13sp"
    fontWeight: 500
  tag:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 500
rounded:
  tag: "7dp"
  row: "12dp"
  control: "14dp"
  card: "16dp"
  panel: "20dp"
  frame: "20dp"
  sheet: "28dp"
  pill: "50%"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  gutter: "20dp"
  section: "16dp"
  xl: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.background}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    height: "52dp"
  button-green:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.control}"
    height: "52dp"
  button-outlined:
    backgroundColor: "{colors.background}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.control}"
    height: "52dp"
  chip:
    backgroundColor: "transparent, {colors.ink} when chosen"
    textColor: "{colors.text-primary}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    height: "40dp"
  input:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.control}"
    height: "52dp"
  panel:
    backgroundColor: "{colors.panel}"
    rounded: "{rounded.panel}"
  card-tinted:
    backgroundColor: "{colors.brand-surface}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.card}"
  card-forest:
    backgroundColor: "{colors.forest}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.card}"
  photo-frame:
    rounded: "{rounded.frame}"
  bottom-sheet:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.sheet}"
---

# PlantMate design system

Source of truth for every PlantMate screen. Everyone working on the project follows this file; the Android resources implement it: `app/src/main/res/values/colors.xml` and `values-night/colors.xml` (colours), `values/styles.xml` (`TextAppearance.PlantMate.*`, `Widget.PlantMate.*`, `ShapeAppearance.PlantMate.*`), `values/themes.xml`, `values/dimens.xml`. Product truth and constraints live in `PRODUCT.md` and `AGENTS.md`.

## Overview

A calm, bright, mature app for 20–35 year-old plant owners in Vietnamese cities. The plant is the hero: real photographs in rounded frames carry the colour; the interface stays white, ink and one brand green. Give Away and Trade lead; selling is present but quiet. Mode: Operate — scanability, native Material 3 behaviour and consistency come before decoration.

The approved visual reference is **canvas B2 v3.2** (bold ink and green): ink primary buttons, grey panels instead of outlined cards, ink-selected chips, underline tabs, and a docked white navigation bar with a raised ink Doctor button.

Rejected directions (do not reintroduce): forest + lime palette, cream "atelier" grounds, Bricolage Grotesque or other quirky display faces, hand-drawn or 3D clay illustrations, rotating photo orbits, all-caps eyebrow labels.

## Colors

- **Ground:** white `background` in light; `#0E100F` in dark. Grouping uses the grey `panel` (`color_panel` `#F3F4F3`, dark `#191C1A`) with `panel-line` hairlines between rows, or the green tint (`color_primary_tint` `#E6F2EA`) for Mầm and help notes. Never a border on every block.
- **Ink:** `color_text_primary` `#111412` (dark `#EEF1EE`) is also the fill of primary buttons, chosen chips, member bubbles, the raised Doctor button and the "still available?" card. Text on ink uses `color_background`, which inverts with the theme.
- **Brand green:** `primary` `#1F7A45` (dark `#63C98E`) for the second action (`Widget.PlantMate.Button.Green`: "Vườn của tôi", "Tưới", Doctor "Phân tích"), done states, links, progress and the Give Away type.
- **Rank panel:** `color_rank_panel` `#CFE5D6` (dark `#1E3A29`), only behind the Profile reputation level.
- **Forest:** `forest` `#1F7A45` stays dark green in **both** themes, for the rare green panel that carries white text.
- **Text:** `text-primary` ink `#111412`, `text-secondary` `#5B625D`.
- **Listing types:** Tặng = green tag with white text; Đổi = white tag with ink text; Bán = ink tag with page-colour text. Status: active green, reserved white with a clock, rehomed and hidden grey.
- **Danger** `#B3261E` only for destructive actions, errors and the like heart.

Rule learned the hard way: **never put white or `on_green` text on `color_primary`** — in dark theme it turns light and the text disappears. White text goes on `color_forest`; text on `color_primary` uses `color_on_primary`.

## Typography

One face: **Roboto**, the Android system `sans-serif` (no bundled font). Weights: 700 headings, 500 labels and controls (`sans-serif-medium` where a widget ignores `textFontWeight`, e.g. chips), 400 body. Roboto has no 600 — do not use it.

| Role | Style | Size / weight |
|---|---|---|
| Hero (sign-in headline) | `TextAppearance.PlantMate.Hero` | 30sp / 700 |
| Screen title (tab header) | `ScreenTitle` | 26sp / 700 |
| Detail title | `DetailTitle` | 24sp / 700 |
| Section title | `SectionTitle` | 19sp / 700 |
| Card title | `CardTitle` | 17sp / 500 |
| Body | `Body` | 15sp / 400 |
| Label, buttons | `Label` | 14sp / 500 |
| Meta (area, time) | `Meta` | 13sp / 500 |
| Tag, chips on photos | `Tag` | 12sp / 500 |

- Section titles are **sentence case** ("Mới tặng gần bạn"), 19sp, with "Xem tất cả →" on the right. Never all caps, never a small label above a heading.
- Tracking no tighter than -0.02em. Multi-line Vietnamese headings need room for stacked marks (ổ, ớ): keep the font's own leading or an explicit `app:lineHeight` of at least 1.25 × size.
- Body text minimum 14sp, small text minimum 12sp.
- Every user-visible string lives in `values/strings.xml` **and** `values-vi/strings.xml`. No literal text in layouts or `setText("…")`, no string concatenation in `setText` — use format placeholders and plurals.

## Layout

- **Gutter:** 20dp left and right on every screen (`spacing.gutter`). Horizontal carousels start at 20dp and bleed off the right edge.
- **Rhythm:** 4/8dp grid. 16dp between sections, 8–12dp inside a group, more space above a heading than below it.
- **Tab headers:** a 52dp icon row, then the 30sp title (Garden, Doctor, Community) or a 17sp title row (Profile).
- **Primary navigation (nav canvas B, 2026-10-21):** exactly five tabs in this order — Trang chủ, Vườn, Bác sĩ, Cộng đồng, Hồ sơ — on a white bar docked to the bottom (72dp, hairline on top, `nav_surface`/`nav_line`). Every tab is labelled; the selected one is forest `nav_active_ink` (lime at night) with a filled icon (`ic_nav_*_line` / `_fill`). Bác sĩ is a raised 66dp round button (`nav_doctor_bg` ink with a lime scan-leaf icon; lime with a dark icon at night) ringed in the bar colour. Inbox, Settings and AI are secondary screens.
- **Light/dark switch (Profile header, 2026-10-21):** a sun/moon pill replaces the settings gear; the chosen half is ink with a lime icon (lime with a dark icon at night). It applies at once and is saved per account (`SettingsRepository.KEY_NIGHT_MODE`); until a member chooses, and when signed out, the app follows the system. Settings stays in the Account section.
- **Touch targets:** 48dp minimum, 8dp apart. A tab's main action (Đăng tin, Thêm cây, Đăng bài) is a small ink button with a + icon beside the tab title (`Widget.PlantMate.Button.Header`), not a floating button, so it never covers content (2026-10-16, QA BUG-009).
- **Edge to edge:** content pads for status, navigation and IME insets. Photo heroes (listing and plant detail) run under the status bar with light icons; the Doctor tab's dark block extends under the status bar.

## Elevation & Depth

Flat by default: hierarchy comes from colour fields and spacing. Panels use the grey `panel` fill, no border and no shadow. Shadows only where something floats: the raised Doctor button. Bottom sheets and dialogs use Material 3 tonal elevation.

## Shapes

- Buttons, inputs and search: 14dp (`corner_radius_control`); small buttons 12dp.
- Panels: 16dp for rows and notes, 20dp for large panels. Tags: 7dp.
- Photo frames: 16dp in grids and the Home featured strip (160×220dp cards, dark fade under the name and sharer), 20–24dp for heroes. Avatars: circles.
- Chips and chat bubbles: pills; bubbles keep a 6dp tail corner.
- Sale status pill (sale listings only, under the card text): a capsule with a 12dp icon, 12sp bold text,
  tinted per status: Available (tick, `sale_available_*`), Reserved (clock, `sale_reserved_*`), Sold (double
  tick, `sale_sold_*`, photo turns grey, price unchanged). Light and dark values in `values(-night)/colors.xml`.
- Bottom sheets: 28dp top corners. The sign-in brand block: 36dp bottom corners; its photo tiles: 14dp.

## Components

- **Buttons:** 52dp, 14dp corners. Ink primary (`Widget.PlantMate.Button`) for the screen's main action; green (`Button.Green`) for the second; outlined 1dp ring for the rest; text for tertiary. On an ink panel: white (`Button.OnInk`) and a faint white ring. Google / Facebook buttons keep their brand icons untinted.
- **Chips:** 40dp pills with a hairline ring; chosen = ink fill, page-colour text, no check (`PlantMateFilterChip`).
- **Inputs:** grey `panel` fill, no stroke, 2dp ink ring when focused.
- **Tabs:** underline tabs (`Widget.PlantMate.UnderlineTabs`) over a hairline, aligned to the 20dp gutter.
- **Listing card:** photo in a 16dp frame with the type tag top-left, name (`Label` 15sp), the offer line (green "Miễn phí", "Đổi lấy …", bold price) and the owner. Grid of 2 columns.
- **Panels:** grey panel with hairline rows (account menu, pickup, care plan); green-tint note for Mầm; rank panel for the Profile level; ink panel for "Cây còn không?".
- **Chat:** member bubbles ink, other side grey panel; composer = grey mic circle, grey pill input, ink send circle; one muted safety line with a shield.
- **Owner card (listing detail, canvas option B):** deep green panel (`owner_card_bg`, 20dp corners; a lighter green in dark theme) with light text and muted secondary text, a white avatar disc, a white "Xem trang cá nhân" button with green text, and the three stats in light text over faint dividers.
- **Garden month sheet (garden month canvas B):** tapping the big date opens a bottom sheet with ‹ month ›, a six-row Monday-first grid using the week strip's dots (solid green planned, green ring done, grey ring none), a legend, and the selected day's care in a grey panel; the selected day is an ink rounded square.
- **Comment threads (option A):** two levels only; every comment has "Trả lời", answering a reply stays in the thread and starts with a green bold "@Name"; replies start collapsed behind "⌄ Xem N câu trả lời".
- **Care heatmap icons (style 1):** watered days blue (`care_water`) with a white drop, fertilised days earth orange (`care_feed`) with a white fertiliser bag; the legend and the plant's water/fertiliser tiles use the same icons. A day with both is split diagonally (blue drop top-left, orange bag bottom-right, legend "Cả hai"); tapping a day lists its care below the legend.
- **Notifications (bell canvas A):** a bell is the third icon on the Home header with a red count pill (`notif_badge`, ringed in the page colour, "9+" above nine). The screen has "Đọc hết", All / Care / Community chips and Today / Earlier groups; rows have a 40dp coloured icon disc (water blue, mention green, reply/comment ink, like `notif_like`, friend `notif_friend`), the member's name in bold, a meta line, and a light green fill (`notif_unread_bg`) with a green dot while unread.
- **Messages (DM canvas):** the chat buttons on Home and Community carry the same red count pill as the bell (chats with new messages). Inbox rows with new messages get a green dot after the time and a bold, ink preview; plant-less chats hide the plant pill. Member profiles show the friend button outlined in white and "Nhắn tin" filled white, side by side.
- **Home sort menu (sort canvas A):** "Mới nhất ▾" beside "Tin mới quanh bạn" opens a small card menu (18dp corners, raised) with Mới nhất / Phù hợp nhất / Gần bạn nhất, each with a one-line reason; the chosen row is light green with a green tick and the label shows it. The order is not a filter (no badge). The filter sheet beside search holds only pick-up areas and "with photos"; the type chips own Give Away / Trade / Sell.
- **Member profile (canvas D):** the identity card is ink (`member_hero_bg`, a shade lighter in dark theme) with 22dp corners: avatar with a lime ring, name, "area · từ MM/yyyy", a 56dp lime trust ring with the score top right, verification badges (lime with a tick when verified, dashed and dimmed when not), the bio as an italic quote, stats in one row with thin dividers, then the friend button outlined in white and "Nhắn tin" in lime, one line each. Under it a mint row gives the level's advice (amber shield for "take care").
- **Admin dashboard (canvas X):** paper page with a soft mint glow top right; a kicker and one 24sp summary sentence with lime-marked numbers; an ink card for waiting reports (40sp count, previews over hairlines, lime full-width button); grey panel cells (20dp corners) for activity and counts; the newest listing as a 160dp photo card with a dark bottom shade and lime kicker; then "Thống kê" (two donuts, a topic bar, report and member lists in grey cells), outlined shortcut buttons. Charts use brand colours only - no rainbow palette.
- **Trust panel (Profile, trust canvas 1):** the rank panel holds a 60dp score ring with the number inside, the level and a hint naming the next missing check, then `item_trust_row` rows (label, "+points", a tick or a small black "Xác thực" button), a divider and three history rows. The same row is used in the sell-check bottom sheet without points. On the owner card, verified checks show as translucent pills with a tick and the level as a light-green pill (`owner_card_level`).
- **Story card (Community, canvas option C):** narrow 110×174dp photo card, no autoplay: the author's 32dp round avatar top-left, their name on a dark fade at the bottom (no story title), a 3dp green frame while unseen; "Tạo tin" and "Thêm bạn" are grey cards with a 44dp green icon disc.
- **Logo (2026-10-15):** an open hand holding a leafy sprout whose stem loops into an infinity sign, inside a light-green ring, on deep green `#101D14` (`drawable-nodpi/logo_plantmate.png`; `@drawable/ic_plantmate_brand` shows it at 38dp). The launcher icon uses the same drawing on a `#101D14` background, with a white single-colour layer for themed icons. Never stretch, recolour or add text to it.
- **States:** every data screen shows loading, content, empty (with an action) and error with retry.

## Do's and Don'ts

**Do**
- Let real plant photos carry colour; keep chrome white and green.
- Reuse `TextAppearance.PlantMate.*`, `Widget.PlantMate.*` and the colour tokens; add a token before adding a literal.
- Test every change in light **and** dark theme, and at 130% and 200% font size.
- Give text views and inputs `wrap_content` height with a `minHeight` (never a fixed `dp` height), so large text grows the view instead of being cut; let badge rows wrap (`ChipGroup`) and share button rows with weights.
- Use data the app actually has; label samples as samples.

**Don't**
- Don't use white text on `color_primary` (use `color_forest`), and don't use white on ink: use `color_background` so it inverts in dark theme.
- Don't write all-caps section titles, eyebrow labels, or `01 / 02` numbering.
- Don't hard-code text, colours or dimensions in layouts or Java.
- Don't add icon-in-circle trios, decorative gradients, glow, glass or 3D illustrations.
- Don't add a sixth tab, payment, cart or shipping flows.
