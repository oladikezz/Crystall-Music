package com.crystall.music.player

import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionStateCodecTest {
    @Test fun roundTripsPausedSessionAndOmitsExpiredRemoteStreams() {
        val track = Track("yt1", "Song", "Artist", source = AudioSource.YOUTUBE, streamUrl = "https://expired")
        val encoded = PlaybackSessionStateCodec.encode(
            PlaybackSessionState(listOf(track), track, 12_345L, true, 2, 1.25f, false)
        )

        val decoded = PlaybackSessionStateCodec.decode(encoded)!!
        assertEquals(listOf(track.copy(streamUrl = null)), decoded.queue)
        assertEquals(track.copy(streamUrl = null), decoded.currentTrack)
        assertEquals(12_345L, decoded.positionMs)
        assertTrue(decoded.isShuffle)
        assertEquals(2, decoded.repeatMode)
        assertEquals(1.25f, decoded.playbackSpeed)
        assertFalse(decoded.isEndlessRadioEnabled)
    }

    @Test fun rejectsCorruptStateAndSanitizesInvalidSettings() {
        assertNull(PlaybackSessionStateCodec.decode("not json"))
        val decoded = PlaybackSessionStateCodec.decode(
            """{"queue":[],"positionMs":-1,"repeatMode":8,"playbackSpeed":99,"isEndlessRadioEnabled":true}"""
        )!!
        assertEquals(0L, decoded.positionMs)
        assertEquals(2, decoded.repeatMode)
        assertEquals(1f, decoded.playbackSpeed)
    }
}
