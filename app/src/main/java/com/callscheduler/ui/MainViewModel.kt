package com.callscheduler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callscheduler.data.model.*
import com.callscheduler.data.repository.CallSchedulerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// ÉNUMÉRATIONS & ÉTAT UI
// ─────────────────────────────────────────────

enum class FilterMode { ALL, ENABLED_ONLY, TODAY, BY_GROUP }
enum class SortMode { BY_TIME, BY_NAME, BY_DATE, BY_STATUS }

data class UiState(
    val calls: List<ScheduledCall> = emptyList(),
    val history: List<CallHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: FilterMode = FilterMode.ALL,
    val sortMode: SortMode = SortMode.BY_TIME,
    val showStatsPanel: Boolean = false,
    val enabledCount: Int = 0,
    val totalCallsMade: Int = 0
)

// ─────────────────────────────────────────────
// VIEWMODEL
// ─────────────────────────────────────────────

class MainViewModel(private val repository: CallSchedulerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllScheduledCalls().collect { calls ->
                _uiState.update { current ->
                    current.copy(
                        calls = calls,
                        isLoading = false,
                        enabledCount = calls.count { it.isEnabled },
                        totalCallsMade = calls.sumOf { it.totalCallsMade }
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getCallHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun toggleCall(call: ScheduledCall) {
        viewModelScope.launch {
            repository.toggleCallEnabled(call.id, !call.isEnabled)
            _toastMessage.value = if (!call.isEnabled) "✅ Appel activé" else "⏸️ Appel désactivé"
        }
    }

    fun deleteCall(call: ScheduledCall) {
        viewModelScope.launch {
            repository.deleteScheduledCall(call.id)
            _toastMessage.value = "🗑️ Appel supprimé"
        }
    }

    fun saveCall(call: ScheduledCall) {
        viewModelScope.launch {
            repository.insertScheduledCall(call)
            _toastMessage.value = "✅ Appel planifié"
        }
    }

    fun updateCall(call: ScheduledCall) {
        viewModelScope.launch {
            repository.updateScheduledCall(call)
            _toastMessage.value = "✅ Appel mis à jour"
        }
    }

    fun duplicateCall(call: ScheduledCall) {
        val newCall = call.copy(
            id = java.util.UUID.randomUUID().toString(),
            label = "${call.label} (copie)",
            totalCallsMade = 0,
            lastCallStatus = CallStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertScheduledCall(newCall)
            _toastMessage.value = "📋 Appel dupliqué"
        }
    }

    fun onFilterChanged(filter: FilterMode) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilterAndSort()
    }

    fun onSortChanged(sort: SortMode) {
        _uiState.update { it.copy(sortMode = sort) }
        applyFilterAndSort()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
    }

    fun toggleStatsPanel() {
        _uiState.update { it.copy(showStatsPanel = !it.showStatsPanel) }
    }

    private fun applyFilterAndSort() {
        _uiState.update { current ->
            var filtered = current.calls

            filtered = when (current.selectedFilter) {
                FilterMode.ALL -> filtered
                FilterMode.ENABLED_ONLY -> filtered.filter { it.isEnabled }
                FilterMode.TODAY -> {
                    val now = System.currentTimeMillis()
                    filtered.filter { it.isEnabled && it.nextCallTimestamp in now..(now + 86_400_000L) }
                }
                FilterMode.BY_GROUP -> filtered
            }

            if (current.searchQuery.isNotEmpty()) {
                val q = current.searchQuery.lowercase()
                filtered = filtered.filter {
                    it.label.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q) ||
                    it.groupTag.lowercase().contains(q)
                }
            }

            filtered = when (current.sortMode) {
                SortMode.BY_TIME -> filtered.sortedBy { it.scheduledHour * 60 + it.scheduledMinute }
                SortMode.BY_NAME -> filtered.sortedBy { it.label.lowercase() }
                SortMode.BY_DATE -> filtered.sortedBy { it.nextCallTimestamp }
                SortMode.BY_STATUS -> filtered.sortedBy { it.lastCallStatus.ordinal }
            }

            current.copy(calls = filtered)
        }
    }
}
