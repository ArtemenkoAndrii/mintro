package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.QualifierListFragmentBinding
import com.mas.mobile.databinding.QualifierListTabBinding
import com.mas.mobile.domain.message.Qualifier
import com.mas.mobile.presentation.adapter.QualifierAdapter
import com.mas.mobile.presentation.adapter.QualifierPagerAdapter
import com.mas.mobile.presentation.viewmodel.QualifierListViewModel

class QualifierListFragment : ListFragment() {
    private lateinit var binding: QualifierListFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.qualifier_list_fragment, container, false)

        binding = QualifierListFragmentBinding.bind(layout)
        binding.qualifierViewPager.adapter = QualifierPagerAdapter(this)
        TabLayoutMediator(binding.qualifierTabLayout, binding.qualifierViewPager) { tab, position ->
            tab.text = when(position) {
                0 -> getResourceService().tabQualifierCatch()
                1 -> getResourceService().tabQualifierSkip()
                2 -> getResourceService().tabQualifierBlacklist()
                else -> null
            }
        }.attach()

        return layout
    }

    override fun resolveAddButtonDestination(): NavDirections {
        TODO("Not yet implemented")
    }
}

open class QualifierTabFragment(private val type: Qualifier.Type) : ListFragment() {
    private lateinit var binding: QualifierListTabBinding

    val listViewModel: QualifierListViewModel by lazyViewModel {
        this.requireContext().appComponent.qualifierListViewModel().create(type)
    }

    // Because, during rotation, the fragment needs to be recreated by invoking the non-argument constructor.
    constructor() : this(Qualifier.Type.CATCH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.qualifier_list_tab, container, false)

        val adapter = QualifierAdapter(this)
        binding = QualifierListTabBinding.bind(layout)
        binding.qualifierList.adapter = adapter
        binding.qualifierList.layoutManager = LinearLayoutManager(this.requireContext())
        binding.model = listViewModel
        binding.lifecycleOwner = this

        listViewModel.qualifiers.observe(viewLifecycleOwner) {
            adapter.setItems(it)
        }

        binding.qualifierAddBtn.setOnClickListener {
            listViewModel.addNew()
        }

        return layout
    }

    fun saveQualifier(item: Qualifier) { listViewModel.save(item) }
    fun removeQualifier(item: Qualifier) { listViewModel.remove(item) }

    override fun resolveAddButtonDestination(): NavDirections {
        TODO("Not yet implemented")
    }
}

class BlackListFragment : QualifierTabFragment(Qualifier.Type.BLACKLIST)
