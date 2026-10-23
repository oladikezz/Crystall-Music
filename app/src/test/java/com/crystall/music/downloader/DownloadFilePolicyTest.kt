package com.crystall.music.downloader

import com.crystall.music.data.model.AudioSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadFilePolicyTest {
    @Test
    fun sanitizedIdsCannotCreateNestedPaths() {
        assertEquals("artist___track", DownloadFilePolicy.sanitizeTrackId("artist/../track"))
        assertFalse(DownloadFilePolicy.sanitizeTrackId("../track").contains('/'))
    }

    @Test
    fun fileNamesUseSourceAndStreamType() {
        assertEquals("id.mp3", DownloadFilePolicy.targetFile(File("/music"), "id", AudioSource.SOUNDCLOUD, "https://host/audio").name)
        assertEquals("id.webm", DownloadFilePolicy.targetFile(File("/music"), "id", AudioSource.YOUTUBE, "https://host/audio?itag=251").name)
        assertEquals("id.m4a", DownloadFilePolicy.targetFile(File("/music"), "id", AudioSource.YOUTUBE, "https://host/audio").name)
    }

    @Test
    fun partialFileIsSeparateAndSizeMustBeCompleteAndNonEmpty() {
        val target = File("/music/id.m4a")
        assertEquals("id.m4a.part", DownloadFilePolicy.partialFile(target).name)
        assertTrue(DownloadFilePolicy.hasValidSize(12, 12))
        assertTrue(DownloadFilePolicy.hasValidSize(12, -1))
        assertFalse(DownloadFilePolicy.hasValidSize(0, -1))
        assertFalse(DownloadFilePolicy.hasValidSize(11, 12))
        assertFalse(DownloadFilePolicy.hasValidSize(13, 12))
    }
}
