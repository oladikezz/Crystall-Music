package com.crystall.music.ui

import com.crystall.music.data.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackSearch(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> List<Track>,
    private val debounceMs: Long = 300
) {
    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results = _results.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched = _hasSearched.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private var query = ""

    fun submit(value: String, immediately: Boolean = false) {
        query = value.trim()
        val request = ++generation
        val requestedQuery = query
        job?.cancel()
        _failed.value = false
        _results.value = emptyList()
        _hasSearched.value = requestedQuery.isNotEmpty()
        _isSearching.value = requestedQuery.isNotEmpty()
        if (requestedQuery.isEmpty()) return
        job = scope.launch {
            try {
                if (!immediately) delay(debounceMs)
                val tracks = fetch(requestedQuery)
                if (request == generation) _results.value = tracks
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (request == generation) _failed.value = true
            } finally {
                if (request == generation) _isSearching.value = false
            }
        }
    }

    fun retry() = submit(query, immediately = true)
}
