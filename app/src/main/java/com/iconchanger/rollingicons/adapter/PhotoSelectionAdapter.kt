package com.iconchanger.rollingicons.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.iconchanger.rollingicons.R
import java.io.File

class PhotoSelectionAdapter(
    private val selectedPhotosList: List<String>,
    private val onAddPhotoClick: () -> Unit,
    private val onDeletePhotoClick: (photoIndex: Int) -> Unit
) : RecyclerView.Adapter<PhotoSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoContainer: View = view.findViewById(R.id.photo_container)
        val addContainer: View = view.findViewById(R.id.add_container)
        val imgPhoto: ImageView = view.findViewById(R.id.imgPhoto)
        val btnDeletePhoto: View = view.findViewById(R.id.btnDeletePhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.addContainer.visibility = View.VISIBLE
            holder.photoContainer.visibility = View.GONE
            holder.itemView.setOnClickListener {
                onAddPhotoClick()
            }
        } else {
            holder.addContainer.visibility = View.GONE
            holder.photoContainer.visibility = View.VISIBLE

            val photoIndex = position - 1
            val uriString = selectedPhotosList[photoIndex]
            val imgFile = File(uriString)
            if (imgFile.exists()) {
                holder.imgPhoto.load(imgFile) {
                    scale(coil.size.Scale.FILL)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            } else {
                holder.imgPhoto.load(Uri.parse(uriString)) {
                    scale(coil.size.Scale.FILL)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            }

            holder.btnDeletePhoto.setOnClickListener {
                onDeletePhotoClick(photoIndex)
            }
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = selectedPhotosList.size + 1
}
