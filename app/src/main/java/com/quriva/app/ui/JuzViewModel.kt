package com.quriva.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quriva.app.data.QuranRepository
import com.quriva.app.model.JuzMeta
import com.quriva.app.model.SurahMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface JuzUiState {
    data object Loading : JuzUiState
    data class Success(
        val juzs: List<JuzMeta>,
        val surahs: List<SurahMeta>
    ) : JuzUiState
    data class Error(val message: String) : JuzUiState
}

class JuzViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<JuzUiState>(JuzUiState.Loading)
    val state: StateFlow<JuzUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = JuzUiState.Loading
            _state.value = try {
                val data = withContext(Dispatchers.IO) {
                    QuranRepository.getJuzs(getApplication()) to
                        QuranRepository.getSurahs(getApplication())
                }
                JuzUiState.Success(data.first, data.second)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                JuzUiState.Error(t.message ?: "Daftar juz gagal dimuat.")
            }
        }
    }
}

class JuzViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JuzViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JuzViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
