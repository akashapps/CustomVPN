package com.akash.customvpn

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object DomainRepository {
    private const val PREFS_NAME = "VpnPrefs"
    private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
    private var prefs: SharedPreferences? = null

    private val _discoveredDomains = MutableLiveData<Set<String>>(emptySet())
    val discoveredDomains: LiveData<Set<String>> = _discoveredDomains

    private val _blockedDomains = MutableLiveData<Set<String>>(emptySet())
    val blockedDomains: LiveData<Set<String>> = _blockedDomains

    private val _filterText = MutableLiveData<String>("")
    val filterText: LiveData<String> = _filterText

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDomains = prefs?.getStringSet(KEY_BLOCKED_DOMAINS, emptySet()) ?: emptySet()
        _blockedDomains.value = savedDomains
    }

    fun addDiscoveredDomain(domain: String) {
        val currentDiscovered = _discoveredDomains.value ?: emptySet()
        if (!currentDiscovered.contains(domain)) {
            _discoveredDomains.postValue(currentDiscovered + domain)
        }

        // Automatically block Samsung domains on discovery
        if (domain.contains("samsung", ignoreCase = true) ||
            domain.contains("knox", ignoreCase = true) ||
            domain.contains("t-mobile", ignoreCase = true)
        ) {
            val currentBlocked = (_blockedDomains.value ?: emptySet()).toMutableSet()
            if (!currentBlocked.contains(domain)) {
                currentBlocked.add(domain)
                _blockedDomains.postValue(currentBlocked)
                prefs?.edit {
                    putStringSet(KEY_BLOCKED_DOMAINS, currentBlocked)
                }
            }
        }
    }

    fun toggleBlockDomain(domain: String) {
        val current = (_blockedDomains.value ?: emptySet()).toMutableSet()
        if (current.contains(domain)) {
            current.remove(domain)
        } else {
            current.add(domain)
        }
        _blockedDomains.postValue(current)
        prefs?.edit {
            putStringSet(KEY_BLOCKED_DOMAINS, current)
        }
    }

    fun setFilterText(text: String) {
        _filterText.postValue(text)
    }

    fun isBlocked(domain: String): Boolean {
        return _blockedDomains.value?.contains(domain) == true
    }

    fun clearDiscoveredDomains() {
        _discoveredDomains.postValue(emptySet())
    }
}
