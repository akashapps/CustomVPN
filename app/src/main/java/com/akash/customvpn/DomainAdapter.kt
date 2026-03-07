package com.akash.customvpn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DomainAdapter(
    private var domains: List<String>,
    private val onBlockToggled: (String) -> Unit
) : RecyclerView.Adapter<DomainAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
        val checkBox: CheckBox = view.findViewById(android.R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.domain_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val domain = domains[position]
        holder.textView.text = domain
        holder.checkBox.isChecked = DomainRepository.isBlocked(domain)
        holder.checkBox.setOnCheckedChangeListener { _, _ ->
            onBlockToggled(domain)
        }
    }

    override fun getItemCount() = domains.size

    fun updateData(newDomains: List<String>) {
        domains = newDomains
        notifyDataSetChanged()
    }
}
