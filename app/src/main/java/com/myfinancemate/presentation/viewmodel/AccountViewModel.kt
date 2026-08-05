package com.myfinancemate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myfinancemate.data.local.entity.AccountEntity
import com.myfinancemate.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val primaryAccountId: Long? = null,
    val message: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountUiState()
        )

    init {
        observeAccounts()
    }

    private fun observeAccounts() {
        accountRepository.getAll()
            .catch { _uiState.update { it.copy(message = it.message ?: "Failed to load accounts") } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            .let { flow ->
                viewModelScope.launch {
                    flow.collect { accounts ->
                        _uiState.update {
                            it.copy(
                                accounts = accounts,
                                primaryAccountId = accounts.firstOrNull { a -> a.isPrimary }?.id
                            )
                        }
                    }
                }
            }
    }

    fun setPrimary(accountId: Long) {
        viewModelScope.launch {
            runCatching { accountRepository.setPrimary(accountId) }
                .onSuccess { _uiState.update { it.copy(message = "Primary account updated") } }
                .onFailure { _uiState.update { it.copy(message = "Failed to set primary account") } }
        }
    }

    fun addAccount(name: String, bankName: String = "", senderId: String) {
        if (name.isBlank() || senderId.isBlank()) {
            _uiState.update { it.copy(message = "Name and sender ID are required") }
            return
        }
        viewModelScope.launch {
            val isFirst = accountRepository.count() == 0
            val account = AccountEntity(
                name = name.trim(),
                bankName = bankName.trim(),
                senderId = senderId.trim(),
                isPrimary = isFirst
            )
            runCatching { accountRepository.insert(account) }
                .onSuccess { _uiState.update { it.copy(message = "Account added") } }
                .onFailure { _uiState.update { it.copy(message = "Failed to add account") } }
        }
    }

    fun renameAccount(accountId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val account = accountRepository.getById(accountId) ?: return@launch
            runCatching { accountRepository.update(account.copy(name = newName.trim())) }
                .onSuccess { _uiState.update { it.copy(message = "Account renamed") } }
                .onFailure { _uiState.update { it.copy(message = "Failed to rename account") } }
        }
    }

    fun deleteAccount(accountId: Long) {
        if (accountId == _uiState.value.primaryAccountId) {
            _uiState.update { it.copy(message = "Cannot delete the primary account") }
            return
        }
        viewModelScope.launch {
            val account = accountRepository.getById(accountId) ?: return@launch
            runCatching { accountRepository.delete(account) }
                .onSuccess { _uiState.update { it.copy(message = "Account deleted") } }
                .onFailure { _uiState.update { it.copy(message = "Failed to delete account") } }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
