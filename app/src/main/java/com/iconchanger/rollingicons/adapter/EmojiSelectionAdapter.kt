package com.iconchanger.rollingicons.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iconchanger.rollingicons.R

class EmojiSelectionAdapter(
    private val emojiList: List<String>,
    private val selectedEmojisSet: MutableSet<String>,
    private val emojiAppBindingsMap: Map<String, String>,
    private val onLinkAppClick: (String, Int, String?) -> Unit,
    private val canSelectMore: (() -> Boolean)? = null,
    private val onItemClick: ((String, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<EmojiSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.emoji_item_icon_view)
        val checkedView: ImageView = view.findViewById(R.id.emoji_item_icon_checked_view)
        val btnLinkApp: View = view.findViewById(R.id.btnLinkApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val emojiName = emojiList[position]
        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(emojiName, "drawable", context.packageName)

        if (resId != 0) {
            holder.iconView.setImageResource(resId)
        } else {
            holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val isSelected = selectedEmojisSet.contains(emojiName)
        holder.itemView.isSelected = isSelected
        holder.checkedView.setImageResource(if (isSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)

        val boundPackage = emojiAppBindingsMap[emojiName]
        val isBound = !boundPackage.isNullOrEmpty()
        if (isBound) {
            holder.btnLinkApp.setBackgroundResource(R.drawable.bg_circle_bound_link)
        } else {
            holder.btnLinkApp.setBackgroundResource(R.drawable.bg_circle_unbound_link)
        }

        holder.btnLinkApp.setOnClickListener {
            onLinkAppClick(emojiName, resId, boundPackage)
        }

        holder.itemView.setOnClickListener {
            val currentlySelected = selectedEmojisSet.contains(emojiName)
            if (!currentlySelected && canSelectMore != null && !canSelectMore.invoke()) {
                return@setOnClickListener
            }

            val nowSelected = if (currentlySelected) {
                selectedEmojisSet.remove(emojiName)
                false
            } else {
                selectedEmojisSet.add(emojiName)
                true
            }
            holder.itemView.isSelected = nowSelected
            holder.checkedView.setImageResource(if (nowSelected) R.drawable.checkbox_selected else R.drawable.checkbox_unselected)
            onItemClick?.invoke(emojiName, nowSelected)
        }
    }

    override fun getItemCount(): Int = emojiList.size
}
