package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.fragment.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.CategoryListRowBinding
import com.mas.mobile.domain.budget.Category
import com.mas.mobile.presentation.activity.fragment.CategoryListFragment
import com.mas.mobile.presentation.activity.fragment.CategoryListFragmentDirections
import com.mas.mobile.presentation.viewmodel.validator.Action

class CategoryAdapter(
    private val fragment: CategoryListFragment
): BaseAdapter<Category, CategoryListRowBinding>(R.layout.category_list_row) {
    override fun bind(binding: CategoryListRowBinding, item: Category, prior: Category?) {
        binding.model = item

        binding.categoryRowIcon.setImageDrawable(fragment.getDrawable(item.iconId))

        binding.layoutRowCard.setOnClickListener {
            val action = CategoryListFragmentDirections.actionToCategory(Action.VIEW.name, item.id.value)
            fragment.findNavController().navigate(action)
        }

        binding.callback = View.OnClickListener{ viewMenu ->
            val menu = PopupMenu(viewMenu.context, viewMenu)
            menu.inflate(R.menu.standard_row_menu)
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.standard_row_menu_view -> {
                        val action = CategoryListFragmentDirections.actionToCategory(Action.VIEW.name, item.id.value)
                        fragment.findNavController().navigate(action)
                        true
                    }
                    R.id.standard_row_menu_edit -> {
                        val action = CategoryListFragmentDirections.actionToCategory(Action.EDIT.name, item.id.value)
                        fragment.findNavController().navigate(action)
                        true
                    }
                    R.id.standard_row_menu_clone -> {
                        val action = CategoryListFragmentDirections.actionToCategory(Action.CLONE.name, item.id.value)
                        fragment.findNavController().navigate(action)
                        true
                    }
                    R.id.standard_row_menu_remove -> {
                        with(fragment) {
                            showConfirmationDialog(getResourceService().messageAreYouSure()) {
                                listViewModel.remove(item)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            menu.show()
        }
    }

    override fun getBinding(view: View) = CategoryListRowBinding.bind(view)
}