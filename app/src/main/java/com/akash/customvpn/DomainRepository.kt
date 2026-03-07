package com.akash.customvpn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object DomainRepository {
    private val _discoveredDomains = MutableLiveData<Set<String>>(emptySet())
    val discoveredDomains: LiveData<Set<String>> = _discoveredDomains

    private val _blockedDomains = MutableLiveData<Set<String>>(emptySet())
    val blockedDomains: LiveData<Set<String>> = _blockedDomains

    fun addDiscoveredDomain(domain: String) {
        val current = _discoveredDomains.value ?: emptySet()
        if (!current.contains(domain)) {
            _discoveredDomains.postValue(current + domain)
        }
    }

    fun toggleBlockDomain(domain: String) {
        val current = _blockedDomains.value ?: emptySet()
        if (current.contains(domain)) {
            _blockedDomains.postValue(current - domain)
        } else {
            _blockedDomains.postValue(current + domain)
        }
    }

    fun isBlocked(domain: String): Boolean {
        return _blockedDomains.value?.contains(domain) == true
    }
}
