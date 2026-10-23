package com.crystall.music.engine

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeStreamCacheKeyTest {
    @Test fun qualityModeIsPartOfStreamCacheIdentity() {
        assertNotEquals(YouTubeStreamCacheKey("video", true), YouTubeStreamCacheKey("video", false))
        assertEquals(YouTubeStreamCacheKey("video", true), YouTubeStreamCacheKey("video", true))
    }
}
