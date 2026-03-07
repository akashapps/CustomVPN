package com.akash.customvpn

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.akash.customvpn.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var currentFilter: String = ""
    private var adapter: DomainAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DomainAdapter(emptyList()) { domain ->
            DomainRepository.toggleBlockDomain(domain)
        }
        binding.recyclerViewDomains.adapter = adapter

        // Observe discovered domains and filter them
        DomainRepository.discoveredDomains.observe(viewLifecycleOwner) {
            filterAndPopulateList()
        }

        // Observe blocked domains to keep sorting up to date
        DomainRepository.blockedDomains.observe(viewLifecycleOwner) {
            filterAndPopulateList()
        }

        // Filter functionality
        binding.editTextFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentFilter = s.toString()
                filterAndPopulateList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear filter button functionality
        binding.buttonClearFilter.setOnClickListener {
            binding.editTextFilter.text.clear()
        }

        // Clear list button functionality
        binding.buttonClear.setOnClickListener {
            DomainRepository.clearDiscoveredDomains()
        }
    }

    private fun filterAndPopulateList() {
        val discovered = DomainRepository.discoveredDomains.value ?: emptySet()
        val blocked = DomainRepository.blockedDomains.value ?: emptySet()
        
        val allDomains = discovered + blocked
        
        val filteredList = if (currentFilter.isEmpty()) {
            allDomains.toList()
        } else {
            allDomains.filter { it.contains(currentFilter, ignoreCase = true) }
        }

        val sortedList = filteredList.sortedWith(compareByDescending<String> { 
            DomainRepository.isBlocked(it) 
        }.thenBy { it })

        adapter?.updateData(sortedList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        adapter = null
    }
}
