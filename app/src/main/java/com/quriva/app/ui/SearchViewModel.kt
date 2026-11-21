package com.quriva.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quriva.app.data.QuranRepository
import com.quriva.app.model.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    var query by mutableStateOf("")
        private set

    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    var totalResults by mutableIntStateOf(0)
        private set

    var isSearching by mutableStateOf(false)
        private set

    var isIndexReady by mutableStateOf(false)
        private set

    var indexError by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    init {
        loadIndex()
    }

    fun onQueryChange(value: String) {
        query = value.take(80)
        searchJob?.cancel()
        if (query.isBlank()) {
            results = emptyList()
            totalResults = 0
            isSearching = false
            return
        }
        if (query.trim().length < 2) {
            results = emptyList()
            totalResults = 0
            isSearching = false
            return
        }
        if (!isIndexReady) return
        searchJob = viewModelScope.launch {
            delay(220)
            isSearching = true
            try {
                val response = withContext(Dispatchers.IO) {
                    QuranRepository.search(getApplication(), query)
                }
                results = response.results
                totalResults = response.total
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                results = emptyList()
                totalResults = 0
            } finally {
                isSearching = false
            }
        }
    }

    fun retryIndex() {
        loadIndex()
    }

    private fun loadIndex() {
        if (isIndexReady) return
        indexError = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    QuranRepository.prepareSearch(getApplication())
                }
                isIndexReady = true
                if (query.trim().length >= 2) onQueryChange(query)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                indexError = t.message ?: "Indeks pencarian gagal dimuat."
            }
        }
    }
}

class SearchViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
