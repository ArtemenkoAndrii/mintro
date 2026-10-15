package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.navigation.fragment.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.MessageListRowBinding
import com.mas.mobile.domain.message.Message
import com.mas.mobile.presentation.activity.fragment.MessageListFragment
import com.mas.mobile.presentation.activity.fragment.MessageListFragmentDirections
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.util.DateTool
import java.time.LocalDate
import java.time.LocalDateTime


class SpendingMessageAdapter(
    private val fragment: MessageListFragment
): BaseAdapter<Message, MessageListRowBinding>(R.layout.message_list_row) {
    private val today = fragment.getResourceService().constantToday()
    private val yesterday = fragment.getResourceService().constantYesterday()

    override fun bind(binding: MessageListRowBinding, item: Message, prior: Message?) {
        binding.message = item

        bindDateDelimiter(binding, item, prior)
        bindCardView(binding, item)

        // Fixes swipe of reused view holders
        binding.executePendingBindings()
        binding.messageListRowCard.translationX = 0f
    }

    private fun bindDateDelimiter(binding: MessageListRowBinding, item: Message, prior: Message?) {
        if (prior?.receivedAt?.toLocalDate() == item.receivedAt.toLocalDate()) {
            binding.messageListDay.visibility = View.GONE
        } else {
            binding.messageListDay.visibility = View.VISIBLE
        }
        binding.messageListDay.setText(calcRelativeDate(item.receivedAt))
    }

    private fun bindCardView(binding:MessageListRowBinding, item: Message) = with (binding) {
        messageListRowLayout.setOnClickListener { goToSpending(item) }
        messageListRowLayout.setOnLongClickListener {
            goToSpending(item, Action.EDIT)
            true
        }

        messageListCategoryLayout.visibility = View.GONE
        messageListMatchedLayout.visibility = View.GONE
        messageListRecommendedLayout.visibility = View.GONE
        messageListRowOkIcon.visibility = View.GONE
        messageListRowQuestionIcon.visibility = View.GONE
        messageListRowInfoIcon.visibility = View.GONE

        when {
            item.hasSpending() -> {
                val expenditure = fragment.listViewModel.getSpending(item.spendingId!!)?.expenditure
                messageListRowOkIcon.visibility = View.VISIBLE
                messageListCategoryLayout.visibility = View.VISIBLE

                val label = if (expenditure == null) {
                    item.spendingId = null
                    NA
                } else {
                    expenditure.name
                }
                messageListRowCategory.setText(label)
            }
            item.status is Message.Matched -> {
                messageListRowQuestionIcon.visibility = View.VISIBLE
                messageListMatchedLayout.visibility = View.VISIBLE
                messageListMatchedButton.setOnClickListener {
                    goToSpending(item)
                }
            }
            item.status is Message.Recommended -> {
                messageListRowInfoIcon.visibility = View.VISIBLE
                messageListRecommendedLayout.visibility = View.VISIBLE
                messageListAcceptButton.setOnClickListener {
                    goToSpending(item)
                }
                messageListBlacklistButton.setOnClickListener {
                    fragment.blacklist(item)
                }
            }
            else -> {}
        }
    }

    private fun goToSpending(item: Message, defaultAction: Action = Action.VIEW) {
        fragment.listViewModel.markAsRead(item)

        if (item.spendingId != null) {
            MessageListFragmentDirections.actionToExistingSpending(defaultAction.name, item.spendingId!!.value, item.id.value)
        } else {
            MessageListFragmentDirections.actionToNewSpending(Action.ADD.name, item.id.value)
        }.also {
            fragment.findNavController().navigate(it)
        }
    }

    override fun getBinding(view: View) = MessageListRowBinding.bind(view)

    private companion object {
        const val NA = "--"
    }

    private fun calcRelativeDate(value: LocalDateTime): String {
        val date = value.toLocalDate()
        return when {
            date.equals(LocalDate.now()) -> this.today
            date.equals(LocalDate.now().minusDays(1)) -> this.yesterday
            else -> DateTool.dateToAbbreviatedString(date)
        }
    }
}