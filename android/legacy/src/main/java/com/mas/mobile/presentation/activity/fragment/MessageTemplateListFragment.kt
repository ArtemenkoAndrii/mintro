package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.MessageTemplateListFragmentBinding
import com.mas.mobile.presentation.adapter.MessageTemplateAdapter
import com.mas.mobile.presentation.viewmodel.MessageTemplateListViewModel

class MessageTemplateListFragment: ListFragment() {
    private lateinit var binding: MessageTemplateListFragmentBinding

    val listViewModel: MessageTemplateListViewModel by lazyViewModel {
        this.requireContext().appComponent.messageTemplateListViewModel().create("dummy")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.message_template_list_fragment, container, false)

        val adapter = MessageTemplateAdapter(this)
        listViewModel.senders.observeForever {
            adapter.setItems(it)
        }

        binding = MessageTemplateListFragmentBinding.bind(layout)
        binding.list = listViewModel
        binding.messageTemplateList.adapter = adapter
        binding.messageTemplateList.layoutManager = LinearLayoutManager(requireContext())
        binding.messageTemplateList.itemAnimator = null // To enable layout opacity
        binding.messageTemplateBlacklist.setOnClickListener {
            go(MessageTemplateListFragmentDirections.actionToBlackList())
        }
        binding.messageTemplateFilters.setOnClickListener {
            go(MessageTemplateListFragmentDirections.actionToFilters())
        }

        return layout
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun resolveAddButtonDestination(): NavDirections {
        TODO("Not yet implemented")
    }
}