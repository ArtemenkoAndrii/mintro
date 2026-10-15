package com.mas.mobile.domain.analytics

import com.mas.mobile.domain.message.Qualifier

interface Event {
    fun getName(): String
    fun getParams(): Map<Param, Any>

    enum class Param {
        Source, Status, Type
    }
}

data class MessageEvaluated(val status: Status) : Event {
    enum class Status {
        Spent, Pending, Recommended, Rejected
    }

    override fun getName() = "message_evaluated"

    override fun getParams() =
        mapOf(Event.Param.Status to status.name)
}

data class SpendingModified(val status: Status, val fromMessage: Boolean = false) : Event {
    enum class Status {
        Save, Remove
    }

    override fun getName() = "spending_modified"

    override fun getParams() =
        if (fromMessage) {
            mapOf(Event.Param.Status to status.name, Event.Param.Source to "message")
        } else {
            mapOf(Event.Param.Status to status.name, Event.Param.Source to "form")
        }
}

data class ScheduledModified(val status: Status) : Event {
    enum class Status {
        Save, Remove
    }

    override fun getName() = "scheduled_modified"

    override fun getParams() =
        mapOf(Event.Param.Status to status.name)
}

data class MessageTemplateModified(val status: Status) : Event {
    enum class Status {
        Save, Remove
    }

    override fun getName() = "message_template_modified"

    override fun getParams() =
        mapOf(Event.Param.Status to status.name)
}

data class QualifierModified(val status: Status, val type: Qualifier.Type) : Event {
    enum class Status {
        Save, Remove
    }

    override fun getName() = "qualifier_modified"

    override fun getParams() =
        mapOf(Event.Param.Status to status.name, Event.Param.Type to type.name)
}

data class CategoryModified(val status: Status) : Event {
    enum class Status {
        Save, Remove
    }

    override fun getName() = "category_modified"

    override fun getParams() =
        mapOf(Event.Param.Status to status.name)
}

data class MessageTemplateFailed(val status: Status) : Event {
    enum class Status {
        Generation, Evaluation
    }

    override fun getName() = "message_template_failed"

    override fun getParams() =
        mapOf(Event.Param.Status to status.name)
}

object AppUpdateSuggested : Event {
    override fun getName() = "app_update_suggested"
    override fun getParams(): Map<Event.Param, Any> = emptyMap()
}

object ScheduledBudgetNotFound : Event {
    override fun getName() = "scheduled_budget_not_found"
    override fun getParams(): Map<Event.Param, Any> = emptyMap()
}

data class ChartShown(val type: Type) : Event {
    enum class Type {
        Trends, Overspending, Distribution
    }

    override fun getName() = "chart_shown"

    override fun getParams() =
        mapOf(Event.Param.Type to type.name)
}