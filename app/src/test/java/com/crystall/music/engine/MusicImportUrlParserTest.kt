package com.crystall.music.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class MusicImportUrlParserTest {
    @Test fun acceptsTrustedLinksPastedWithSurroundingShareText() {
        val parsed = MusicImportUrlParser.parse("Listen here: https://youtu.be/abcdefghijk?t=12")!!
        assertEquals(ImportProvider.YOUTUBE, parsed.provider)
        assertEquals("abcdefghijk", parsed.videoId)

        assertEquals(
            ImportProvider.SOUNDCLOUD,
            MusicImportUrlParser.parse("Open this https://soundcloud.com/artist/track.")!!.provider
        )
    }

    @Test fun rejectsLookalikeHostsAndUserInfo() {
        assertNull(MusicImportUrlParser.parse("https://youtube.com.evil.example/watch?v=abcdefghijk"))
        assertNull(MusicImportUrlParser.parse("https://evil.example/youtube.com/watch?v=abcdefghijk"))
        assertNull(MusicImportUrlParser.parse("https://youtube.com@evil.example/watch?v=abcdefghijk"))
    }

    @Test fun leavesOrdinarySharedTextAndQueriesUnclassified() {
        assertNull(MusicImportUrlParser.parse("Artist - Song (official video)"))
        assertNotNull(MusicImportUrlParser.parse("Song link: https://music.youtube.com/watch?v=abcdefghijk&list=PL12345"))
    }
}
