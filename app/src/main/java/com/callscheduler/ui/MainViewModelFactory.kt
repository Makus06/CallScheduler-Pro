package com.callscheduler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.callscheduler.data.repository.CallSchedulerRepository

class MainViewModelFactory(private val repository: CallSchedulerRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
