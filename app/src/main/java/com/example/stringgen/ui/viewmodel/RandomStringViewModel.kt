package com.example.stringgen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stringgen.data.model.RandomStringData
import com.example.stringgen.data.repository.RandomStringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RandomStringViewModel(private val repository: RandomStringRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RandomStringUiState())
    val uiState: StateFlow<RandomStringUiState> = _uiState.asStateFlow()

    fun updateStringLength(length: String) {
        _uiState.value = _uiState.value.copy(stringLength = length)
    }

    fun generateRandomString() {
        val lengthString = _uiState.value.stringLength
        val length = lengthString.toIntOrNull()

        if (length == null || length <= 0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid positive number"
            )
            return
        }


        if (length > 1000) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "String length must be 1000 or less"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.generateRandomString(length)
                .onSuccess { randomString ->
                    val updatedList = _uiState.value.generatedStrings.toMutableList()
                    updatedList.add(0, randomString)

                    _uiState.value = _uiState.value.copy(
                        generatedStrings = updatedList,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error: ${exception.message ?: "Unexpected error occurred"}"
                    )
                }
        }
    }

    fun deleteAllStrings() {
        _uiState.value = _uiState.value.copy(generatedStrings = emptyList())
    }

    fun deleteSingleString(stringData: RandomStringData) {
        val updatedList = _uiState.value.generatedStrings.toMutableList()
        updatedList.remove(stringData)
        _uiState.value = _uiState.value.copy(generatedStrings = updatedList)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class RandomStringUiState(
    val stringLength: String = "",
    val generatedStrings: List<RandomStringData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)