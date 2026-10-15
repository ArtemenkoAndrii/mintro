package com.mas.mobile.presentation.activity.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.ViewDataBinding
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.CategoryListFragmentBinding
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.presentation.adapter.BaseAdapter
import com.mas.mobile.presentation.adapter.CategoryAdapter
import com.mas.mobile.presentation.viewmodel.CategoryListViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action


class CategoryListFragment : ListFragment() {
    private lateinit var binding: CategoryListFragmentBinding

    val listViewModel: CategoryListViewModel by lazyViewModel {
        this.requireContext().appComponent.categoryListViewModelModel().create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.category_list_fragment, container, false)

        val adapter = CategoryAdapter(this)
        binding = CategoryListFragmentBinding.bind(layout)
        binding.categoryList.adapter = adapter
        binding.categoryList.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryList.itemAnimator = null // To enable layout opacity

        val itemTouchHelper = ItemTouchHelper(ItemMoveCallback(adapter) { old, new ->
            listViewModel.reorder(old, new)
        })
        itemTouchHelper.attachToRecyclerView(binding.categoryList)

        listViewModel.categories.observe(viewLifecycleOwner) {
            adapter.setItems(it)
        }

        binding.categoryAddBtn.setOnClickListener {
            val action = resolveAddButtonDestination()
            this.findNavController().navigate(action)
        }

        return layout
    }

    fun getDrawable(id: IconId?): Drawable {
        return this.getIconPack().icons[id?.value]?.drawable
            ?: AppCompatResources.getDrawable(requireContext(), R.drawable.ic_circle)!!
    }

    override fun resolveAddButtonDestination() =
        CategoryListFragmentDirections.actionToCategory(Action.ADD.name)
}

class ItemMoveCallback<T, V: ViewDataBinding>(
    private val adapter: BaseAdapter<T, V>,
    private val reorderHandler: (old: List<T>, new: List<T>) -> Unit
) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0 // No swipe
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.moveItems(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            reorderHandler(adapter.getOldItems(), adapter.getItems())
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        TODO("Not yet implemented")
    }

    override fun isLongPressDragEnabled(): Boolean {
        // Allow long press to initiate drag
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        // Disable swipe actions
        return false
    }
}