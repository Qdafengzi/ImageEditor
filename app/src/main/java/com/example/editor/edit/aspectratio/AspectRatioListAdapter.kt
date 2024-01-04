package com.example.editor.edit.aspectratio

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.editor.R
import com.example.editor.databinding.ItemAspectRatioBinding
import com.example.editor.edit.aspectratio.extensions.inflateAdapterItem

class AspectRatioListAdapter : RecyclerView.Adapter<AspectRatioListAdapter.AspectRatioItemViewHolder>() {

    var onItemSelected: ((AspectRatioItemViewState) -> Unit)? = null

    private val aspectRatioList = arrayListOf<AspectRatioItemViewState>()

    fun updateItemList(aspectRatioList: List<AspectRatioItemViewState>) {
        this.aspectRatioList.clear()
        this.aspectRatioList.addAll(aspectRatioList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AspectRatioItemViewHolder =
        AspectRatioItemViewHolder.create(parent, onItemSelected)

    override fun getItemCount(): Int = aspectRatioList.size

    override fun onBindViewHolder(holder: AspectRatioItemViewHolder, position: Int) =
        holder.bind(aspectRatioList[position])

    class AspectRatioItemViewHolder(
        private val binding: ItemAspectRatioBinding,
        private val onItemSelected: ((AspectRatioItemViewState) -> Unit)?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemSelected?.invoke(binding.viewState!!)
            }
        }

        fun bind(aspectRatioItemViewState: AspectRatioItemViewState) {
            binding.viewState = aspectRatioItemViewState
            binding.executePendingBindings()
        }

        companion object {

            fun create(
                parent: ViewGroup,
                onItemSelected: ((AspectRatioItemViewState) -> Unit)?
            ): AspectRatioItemViewHolder {
                val binding: ItemAspectRatioBinding = parent.inflateAdapterItem(R.layout.item_aspect_ratio)
                return AspectRatioItemViewHolder(binding, onItemSelected)
            }
        }
    }
}