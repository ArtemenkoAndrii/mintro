package com.mas.mobile.presentation.activity.fragment

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mas.mobile.NavGraphDirections
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.MessageListFragmentBinding
import com.mas.mobile.databinding.MessageListRowBinding
import com.mas.mobile.domain.message.Message
import com.mas.mobile.presentation.adapter.BaseAdapter
import com.mas.mobile.presentation.adapter.SpendingMessageAdapter
import com.mas.mobile.presentation.viewmodel.MessageListViewModel

class MessageListFragment: ListFragment() {
    private lateinit var binding: MessageListFragmentBinding

    val listViewModel: MessageListViewModel by lazyViewModel {
        this.requireContext().appComponent.messageListViewModel().create("dummy")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.message_list_fragment, container, false)

        val adapter = SpendingMessageAdapter(this)
        binding = MessageListFragmentBinding.bind(layout)
        binding.messageList.adapter = adapter
        binding.messageList.layoutManager = LinearLayoutManager(this.requireContext())

        val callback = SpendingMessageSwipeCallback { message, position ->
            showConfirmationDialog(
                getResourceService().messageAreYouSure(),
                {
                    listViewModel.remove(message)
                    adapter.notifyItemRemoved(position)
                },
                {
                    adapter.notifyItemChanged(position)
                }
            )
        }
        ItemTouchHelper(callback).also { it.attachToRecyclerView(binding.messageList) }

        listViewModel.messages.observe(viewLifecycleOwner) { message ->
            adapter.setItems(message)
        }

        binding.messageAddBtn.visibility = View.GONE
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showDialog()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.spending_message_list_menu_template -> go(MessageListFragmentDirections.actionToMessageTemplates())
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun blacklist(item: Message) {
        val template = getResourceService().dialogBlacklist()
        val message = String.format(template, item.sender)

        showConfirmationDialog(message) {
            listViewModel.blacklist(item)
        }
    }

    private fun showDialog() {
        if (listViewModel.suggestEnableCapturing()) {
            showConfirmationDialog(getResourceService().messageCapturingDisabled()) {
                this.findNavController().navigate(NavGraphDirections.actionToSettings(true))
            }
        }
    }

    override fun resolveAddButtonDestination(): NavDirections {
        TODO("Not yet implemented")
    }
}

class SpendingMessageSwipeCallback(
    val handler: (Message, Int) -> Unit
): ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        getBinding(viewHolder)?.message?.let { handler(it, viewHolder.bindingAdapterPosition) }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val xxx = viewHolder.bindingAdapterPosition
        println(xxx)
        val card = getBinding(viewHolder)?.messageListRowCard
        val icon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_trash_bin_24)

        if (card != null && icon != null) {
            val cardTop = viewHolder.itemView.top + card.top
            val iconMargin = (card.height - icon.intrinsicHeight) / 2
            val iconTop = cardTop + (card.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight

            when {
                dX > 0 -> { // Swiping to the right
                    val iconLeft = card.left + iconMargin
                    val iconRight = iconLeft + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
                dX < 0 -> { // Swiping to the left
                    val iconLeft = card.right - iconMargin - icon.intrinsicWidth
                    val iconRight = card.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
                else -> { // No swipe
                    val background = ColorDrawable(Color.TRANSPARENT)
                    background.setBounds(0, 0, 0, 0)
                    background.draw(c)
                }
            }

            icon.draw(c)
            val translationX = dX.coerceIn(-card.width.toFloat(), card.width.toFloat())
            card.translationX = translationX
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun getBinding(viewHolder: RecyclerView.ViewHolder): MessageListRowBinding? =
        (viewHolder as? BaseAdapter<Message, MessageListRowBinding>.GroupViewHolder)?.binding
}