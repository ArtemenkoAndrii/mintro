package com.mas.mobile.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

abstract class BaseAdapter<T, V: ViewDataBinding>(private val rowLayout: Int): RecyclerView.Adapter<BaseAdapter<T, V>.GroupViewHolder>() {
    private var items: List<T> = emptyList()
    private var oldItems: List<T> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseAdapter<T, V>.GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(rowLayout, parent, false) as View
        val binding = getBinding(view)
        return GroupViewHolder(binding, view) { item, prior ->
            bind(binding, item, prior)
        }
    }

    abstract fun bind(binding: V, item: T, prior: T?)

    abstract fun getBinding(view: View): V

    override fun onBindViewHolder(holder: BaseAdapter<T, V>.GroupViewHolder, position: Int) = holder.bind(
        items[position], (if (position > 0) items[position-1] else null)
    )

    override fun getItemCount() = items.size

    inner class GroupViewHolder(
        val binding: V,
        val view: View,
        val processor: (item: T, prior: T?) -> Unit
    ): RecyclerView.ViewHolder(view) {

        fun bind(item: T, prior: T?) {
            processor(item, prior)
            // Data Binding defers applying field updates until the next Choreographer frame
            // unless forced - without this, a freshly bound row can briefly show stale/blank
            // content, and a UI test interacting with it right after scrolling into view can
            // race past the update entirely.
            binding.executePendingBindings()
        }
    }

    fun setItems(items: List<T>) {
        val callback = EntityListDiffCallback(this.items, items)
        val result = DiffUtil.calculateDiff(callback)
        result.dispatchUpdatesTo(this)
        this.oldItems = items.toList()
        this.items = items
    }

    fun getItems() = this.items.toList()
    fun getOldItems() = this.oldItems.toList()

    fun moveItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(this.items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
}
