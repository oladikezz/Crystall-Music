# Recon Map: YouTube Music (Android Mobile)

Scope: Core music streaming, playback engine, queue & radio, synchronized lyrics, library, search, downloads, settings
For: Crystall Music (YouTube Music Android Replica)
Date: 2026-10-12

## Sources

| # | source | URL / Reference | notes |
| --- | --- | --- | --- |
| 1 | Google Play Store | https://play.google.com/store/apps/details?id=com.google.android.apps.youtube.music | Screen layout, core features, pitch |
| 2 | YouTube Music Web / App | https://music.youtube.com | UI design system, navigation, player tabs |
| 3 | YouTube Music Help Center | https://support.google.com/youtubemusic | Features: downloads, autoplay, queue, lyrics, audio quality |
| 4 | InnerTube Web Remix API | https://music.youtube.com/youtubei/v1 | Endpoints: search, browse, next, player, lyrics |

## Core Loop

Search or select music from curated feeds or mood chips, stream high-fidelity audio with seamless background playback and lock-screen controls, explore queue / lyrics / related tracks, and download for offline playback.

## Screens

| ID | screen | route / how to reach | purpose | key components | states seen |
| --- | --- | --- | --- | --- | --- |
| S01 | Home (Главная) | Bottom Nav Tab 0 | Personalized feed, quick picks, mood chips, curated charts | TopBar, MoodFilterRow, QuickPicksGrid, TopChartsCarousel, FlowCard | loading, filled, empty, offline |
| S02 | Search (Поиск) | Bottom Nav Tab 1 or TopBar icon | Find songs, artists, playlists, albums, and explore genres | SearchBar, SuggestionList, CategoryChips, TrackResultsList | initial, typing, results, empty |
| S03 | Full Player (Плеер) | Tap MiniPlayer or Track | Immersive playback with song/video toggle, lyrics, queue, related | ArtworkView, SongVideoToggle, Scrubber, TransportControls, UpNextTab, LyricsTab, RelatedTab | buffering, playing, paused, lyrics-loading |
| S04 | Mini Player (Мини-плеер) | Active playback docked above BottomBar | Persistent compact transport controls | RedProgressBar, Thumbnail, TitleMarquee, LikeBtn, PlayPauseBtn, SkipBtn | playing, paused, buffering |
| S05 | Library (Фонотека) | Bottom Nav Tab 3 | Manage downloaded tracks, saved playlists, liked songs | CategoryTabs, SortMenu, DownloadList, PlaylistGrid, FavoritesList | empty, populated, offline |
| S06 | Playlist / Album Detail | Tap playlist or album card | View playlist metadata, track list, download all, shuffle | HeaderCover, ActionRow (Play, Shuffle, DownloadAll), TrackTable | loading, filled |
| S07 | Settings (Настройки) | TopBar gear icon | Audio quality, data saver, autoplay, sleep timer, cache, language | GroupedCardList, ToggleSwitches, SpeedSelector, TimerSelector, ClearCacheBtn | default |

## Flows

```
F01 Quick Pick & Continuous Radio Playback
    S01 tap track -> S03 opens and plays -> S03 switches to Up Next -> seamless autoplay next tracks
    happy path clicks: 1
    edge: network interruption, screen locked, app minimized (handled by ExoPlayer network wakelock)

F02 Search & Play
    S01 tap search icon -> S02 type query -> tap suggestion or result -> S04 / S03 plays
    happy path clicks: 3
    edge: no results found, special characters

F03 Offline Download & Offline Playback
    S03 / S01 tap download -> background stream download -> S05 Downloads tab -> play without internet
    happy path clicks: 2
    edge: low storage, interrupted download, offline mode toggle

F04 Lyrics Exploration
    S03 swipe up or tap LYRICS tab -> synchronized text displays with smooth scrolling
    happy path clicks: 1
    edge: instrumental track, lyrics unavailable

F05 Data Saver & Audio Quality Configuration
    S01 tap Settings -> S07 toggle Data Saver (128kbps Eco vs 320kbps HQ)
    happy path clicks: 2
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| TopBar | standard, search-active | default | S01, S05, S07 |
| MoodChip | default, selected (pure white on black) | normal, pressed | S01 |
| TrackItem | list-row, quick-pick-card | default, active-playing, downloading, downloaded | S01, S02, S05, S06 |
| AlbumCard | square 140x140 | default, playing | S01, S05 |
| MiniPlayer | docked-bar | playing, paused, buffering | S01, S02, S05 |
| FullPlayerSheet | modal-sheet | normal, video-mode, expanded-tabs | S03 |
| RedScrubber | slider with elapsed/total time | idle, dragging | S03, S04 |
| BottomTabsBar | Up Next, Lyrics, Related | active-tab | S03 |
| DockedBottomNav | Home, Search, Import, Library | tab-selected | S01, S02, S05 |

## Inferred Data Model

```
Track        id: String, title: String, artist: String, album: String, coverUrl: String, durationMs: Long, streamUrl: String?, localPath: String?, isDownloaded: Boolean, source: AudioSource
             evidence: YouTube Music track object, InnerTube search result, player endpoint
             confidence: high

Playlist     id: String, title: String, author: String, coverUrl: String?, trackCount: Int, isImported: Boolean
             evidence: YouTube Music playlist page, browseId VL...
             confidence: high

Lyrics       trackId: String, plainLyrics: String?, syncLines: List<SyncLine>?, source: String
             evidence: YouTube Music browseId / lyrics endpoint
             confidence: high

DownloadState trackId: String, progress: Float, isDownloading: Boolean, isCompleted: Boolean, localPath: String
             evidence: SQLite database, Android media store cache
             confidence: high
```

## Feature Matrix

See `replica/features.csv`.

## Out of Scope

- Paid YouTube Premium subscription gate / DRM Widevine L1 video protection (app uses clean open streaming audio formats).
- Google Account OAuth login requirement (clean-room access without requiring users to log into Google).

## Size

Screens: 7, Flows: 5, Entities: 4. Size: M (production Android Jetpack Compose app).
