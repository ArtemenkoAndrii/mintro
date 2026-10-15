package com.mas.mobile.presentation.activity.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.CategoryFragmentBinding
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.presentation.activity.IconPickerActivity
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.presentation.viewmodel.CategoryViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action

class CategoryFragment: ItemFragment<CategoryViewModel>() {
    private val args: CategoryFragmentArgs by navArgs()
    private lateinit var binding: CategoryFragmentBinding
    private lateinit var iconPickerLauncher: ActivityResultLauncher<Intent>

    override val viewModel: CategoryViewModel by lazyViewModel {
        this.requireContext().appComponent.categoryViewModelModel()
            .create(args.categoryId, action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        iconPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val iconId = result.data?.getIntExtra(IconPickerActivity.SELECTED_ICON_ID, -1)
                viewModel.icon.value = iconId?.let { IconId(it) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.category_fragment, container, false)

        action = Action.valueOf(args.action)

        binding = CategoryFragmentBinding.bind(layout)
        binding.model = viewModel
        binding.lifecycleOwner = this

        setUpMerchantsBinding()
        setUpIconBinding()
        setUpEditable()

        binding.categorySaveButton.setOnClickListener {
            saveAndClose(viewModel)
        }
        return layout
    }

    private fun setUpMerchantsBinding() {
        viewModel.merchants.observe(viewLifecycleOwner) { merchants ->
            binding.merchantChipGroup.removeAllViews()
            merchants.forEach { merchant ->
                val chip = Chip(context)
                chip.text = merchant
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener {
                    if (it.isEnabled) {
                        viewModel.removeMerchant(merchant)
                    }
                }
                binding.merchantChipGroup.addView(chip)
            }
        }

        binding.addChipButton.setOnClickListener {
            viewModel.addMerchant()
        }
    }

    private fun setUpIconBinding() {
        binding.categoryIcon.setOnClickListener {
            if (it.isEnabled) {
                val intent = Intent(activity, IconPickerActivity::class.java)
                iconPickerLauncher.launch(intent)
            }
        }

        viewModel.icon.observeForever {
            val drawable = this.getIconPack().icons[it?.value]?.drawable ?: getDefaultIcon()
            binding.categoryIcon.setImageDrawable(drawable)
        }
    }

    private fun setUpEditable() {
        viewModel.isEditable.observe(viewLifecycleOwner) { enabled ->
            binding.merchantChipGroup.children.forEach { it.isEnabled = enabled }
            binding.categoryIcon.isEnabled = enabled
        }
    }

    private fun getDefaultIcon() = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_circle)!!
}
