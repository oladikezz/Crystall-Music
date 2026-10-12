# YouTube Music Component Specifications

## 1. MoodChip (Filter Pill)
- **Variants**: default (unselected), selected (active filter)
- **Sizes**: height 34dp, padding horizontal 14dp, vertical 7dp
- **Tokens**:
  - Unselected: bg `#18FFFFFF`, text `#FFFFFF` 13sp / weight 500, radius `pill` (999dp)
  - Selected: bg `#FFFFFF`, text `#0A0A0A` 13sp / weight 700, radius `pill`
- **States**: default, pressed, selected
- **Used on**: S01 Home

## 2. QuickPickTrackItem
- **Variants**: standard track row inside 4-track column
- **Layout**: Row with 48x48dp square artwork, column with title (14sp) and artist (12sp), 3-dot options menu
- **Tokens**:
  - Artwork: 48x48dp, radius `sm` (4dp), bg `#1B1B1F`
  - Active: EqualizerVisualizer overlay (3 animated bars)
  - Title: 14sp SemiBold, color `#FFFFFF` (active: `#FFFFFF` or red accent)
  - Subtitle: 12sp Regular, color `#AAAAAA`
- **States**: default, active-playing, buffering
- **Used on**: S01 Home

## 3. AlbumCard
- **Variants**: square carousel card (140x140dp)
- **Tokens**:
  - Image: 140x140dp, radius `md` (8dp), shadow `card`
  - Title: 14sp SemiBold, maxLines 1, color `#FFFFFF`
  - Subtitle: 12sp Regular, maxLines 1, color `#AAAAAA`
- **Used on**: S01 Home, S05 Library

## 4. MiniPlayer
- **Variants**: docked edge-to-edge above bottom nav bar
- **Layout**:
  - Top: 1.5dp signature Red Progress Scrubber (`#FF0000`)
  - Row: 44x44dp cover (radius 6dp), title + artist with horizontal marquee, thumbs up / favorite icon, play/pause circular toggle, next track icon
- **Tokens**:
  - Background: `#1B1B1F`, top border: `0.5dp` `#1AFFFFFF`
  - Scrubber: `#FF0000` (track: `#33FFFFFF`)
- **Used on**: Global docked overlay (S01, S02, S05)

## 5. FullPlayerSheet
- **Variants**: modal playback sheet with song/video switcher and bottom tabs
- **Sections**:
  - TopBar: Down arrow (collapse), Song/Video pill toggle (`#1B1B1F` container), options menu
  - Center: High-resolution artwork with subtle drop shadow, animated subtle scale on play/pause
  - Metadata: Title (20sp Bold), Artist (14sp Regular), Thumbs Up, Thumbs Down, Download, Options
  - Scrubber: YouTube signature red seekbar (`#FF0000`) with elapsed & total duration
  - Controls: Shuffle, Previous, Play/Pause (64dp circular button), Next, Repeat (Off/All/One)
  - Bottom Tab Drawer:
    - **UP NEXT**: upcoming queue, endless autoplay switch, drag reorder
    - **LYRICS**: live synchronized or static lyrics with auto-scroll
    - **RELATED**: more from artist, similar tracks, recommended playlists
- **Used on**: S03 Full Player

## 6. DockedBottomNav
- **Variants**: 4-tab docked full-width navigation bar
- **Tabs**: Home, Search, Import, Library
- **Tokens**:
  - Height: 58dp
  - Background: `#0F0F12`
  - Border: 0.5dp top border `#18FFFFFF`
  - Active item: `#FFFFFF` icon + 10sp SemiBold label
  - Inactive item: `#AAAAAA` icon + 10sp Regular label
- **Used on**: Root app shell
