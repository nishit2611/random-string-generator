package com.example.stringgen.ui.viewmodel


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stringgen.data.repository.RandomStringRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RandomStringViewModel::class.java)) {
            val repository = RandomStringRepository(context)
            @Suppress("UNCHECKED_CAST")
            return RandomStringViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}