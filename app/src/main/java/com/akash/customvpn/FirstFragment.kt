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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = DomainAdapter(emptyList()) { domain ->
            DomainRepository.toggleBlockDomain(domain)
        }
        binding.recyclerViewDomains.adapter = adapter

        // Observe discovered domains and filter them
        DomainRepository.discoveredDomains.observe(viewLifecycleOwner) { domains ->
            filterAndPopulateList(domains, adapter)
        }

        // Filter functionality
        binding.editTextFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentFilter = s.toString()
                filterAndPopulateList(DomainRepository.discoveredDomains.value ?: emptySet(), adapter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear button functionality
        binding.buttonClear.setOnClickListener {
            DomainRepository.clearDiscoveredDomains()
        }
    }

    private fun filterAndPopulateList(domains: Set<String>, adapter: DomainAdapter) {
        val filteredList = if (currentFilter.isEmpty()) {
            domains.toList()
        } else {
            domains.filter { it.contains(currentFilter, ignoreCase = true) }
        }
        adapter.updateData(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
