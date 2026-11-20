package com.quriva.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quriva.app.data.QuranRepository
import com.quriva.app.model.SurahDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Success(val detail: SurahDetail) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

class ReaderViewModel(
    application: Application,
    private val surahNumber: Int
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ReaderUiState.Loading
            _state.value = try {
                val detail = withContext(Dispatchers.IO) {
                    QuranRepository.getSurah(getApplication<Application>(), surahNumber)
                }
                if (detail.ayahs.isEmpty()) {
                    ReaderUiState.Error("Ayat surah ini tidak ditemukan.")
                } else {
                    ReaderUiState.Success(detail)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                ReaderUiState.Error(t.message ?: "Gagal memuat surah.")
            }
        }
    }
}

class ReaderViewModelFactory(
    private val application: Application,
    private val surahNumber: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel(application, surahNumber) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
