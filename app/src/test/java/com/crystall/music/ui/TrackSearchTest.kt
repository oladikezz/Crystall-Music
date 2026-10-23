package com.crystall.music.ui

import com.crystall.music.data.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackSearchTest {
    @Test fun debouncesTypingAndClearsOnEmptyQuery() = runTest {
        val queries = mutableListOf<String>()
        val search = TrackSearch(this, { queries += it; emptyList() })
        search.submit("a")
        advanceTimeBy(100)
        search.submit("ab")
        advanceUntilIdle()
        assertEquals(listOf("ab"), queries)
        assertTrue(search.hasSearched.value)
        assertFalse(search.failed.value)
        search.submit("")
        assertFalse(search.hasSearched.value)
        assertFalse(search.isSearching.value)
    }

    @Test fun ignoresStaleResponseEvenWhenFetchDoesNotCancel() = runTest {
        val search = TrackSearch(this, { query ->
            withContext(NonCancellable) { delay(if (query == "old") 1000 else 10) }
            listOf(Track(query, query, "artist"))
        }, debounceMs = 0)
        search.submit("old")
        advanceTimeBy(1)
        search.submit("new")
        advanceUntilIdle()
        assertEquals("new", search.results.value.single().id)
        assertFalse(search.isSearching.value)
    }

    @Test fun distinguishesFailureFromEmptyAndCanRetry() = runTest {
        var fail = true
        val search = TrackSearch(this, { if (fail) error("offline") else emptyList() })
        search.submit("query")
        advanceUntilIdle()
        assertTrue(search.failed.value)
        fail = false
        search.retry()
        advanceUntilIdle()
        assertFalse(search.failed.value)
        assertTrue(search.hasSearched.value)
        assertTrue(search.results.value.isEmpty())
    }
}
