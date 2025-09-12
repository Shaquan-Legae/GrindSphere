package com.example.grindsphere.hustler


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {
    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun setSelectedCategory(category: String) {
        viewModelScope.launch {
            _selectedCategory.value = category
        }
    }

    fun clearSelectedCategory() {
        viewModelScope.launch {
            _selectedCategory.value = ""
        }
    }
}