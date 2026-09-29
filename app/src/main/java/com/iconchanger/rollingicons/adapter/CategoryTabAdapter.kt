package com.iconchanger.rollingicons.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.rollingicons.R

class CategoryTabAdapter(
    private val categories: List<String>,
    private val onCategorySelected: (String, Int) -> Unit
) : RecyclerView.Adapter<CategoryTabAdapter.ViewHolder>() {

    private var selectedIndex = 0

    fun getSelectedCategory(): String {
        return if (selectedIndex in categories.indices) categories[selectedIndex] else ""
    }

    fun setSelectedIndex(index: Int) {
        val prev = selectedIndex
        selectedIndex = index
        if (prev in categories.indices) notifyItemChanged(prev)
        if (selectedIndex in categories.indices) notifyItemChanged(selectedIndex)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutTab: FrameLayout = view.findViewById(R.id.layoutCategoryTab)
        val txtName: TextView = view.findViewById(R.id.txtCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        val context = holder.itemView.context
        val isSelected = position == selectedIndex

        holder.txtName.text = item

        if (isSelected) {
            holder.layoutTab.setBackgroundResource(R.drawable.bg_category_tab_active)
            holder.txtName.setTextColor(ContextCompat.getColor(context, R.color.cosmic_accent))
        } else {
            holder.layoutTab.setBackgroundResource(R.drawable.bg_category_tab_inactive)
            holder.txtName.setTextColor(Color.parseColor("#777788"))
        }

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && selectedIndex != pos) {
                val prev = selectedIndex
                selectedIndex = pos
                notifyItemChanged(prev)
                notifyItemChanged(selectedIndex)
                onCategorySelected(item, selectedIndex)
            }
        }
    }

    override fun getItemCount(): Int = categories.size
}

