package com.mas.mobile.domain.settings

import java.time.LocalDateTime

data class DeferrableAction(
    val key: Key,
    var activeAfter: LocalDateTime = LocalDateTime.now(),
    var increment: Hours = key.getInterval(),
) {
    fun isDeferred() = activeAfter > LocalDateTime.now()

    fun defer() {
        if (!isDeferred()) {
            activeAfter = LocalDateTime.now().plusHours(increment.value)

            if (increment < LIMIT) {
                increment = Hours(increment.value * key.getRatio())
            }
        }
    }

    sealed class Key(val value: String) {
        abstract fun getInterval(): Hours
        abstract fun getRatio(): Int
    }

    @JvmInline
    value class Hours(val value: Long) {
        operator fun compareTo(other: Hours): Int {
            return value.compareTo(other.value)
        }
    }

    companion object {
        val LIMIT = Hours(16 * 24L)
    }
}

class ProgressiveDaily(key: String) : DeferrableAction.Key(key) {
    override fun getInterval(): DeferrableAction.Hours = DeferrableAction.Hours(24L)
    override fun getRatio(): Int = 2
}

class UniformDaily(key: String) : DeferrableAction.Key(key) {
    override fun getInterval(): DeferrableAction.Hours = DeferrableAction.Hours(24L)
    override fun getRatio(): Int = 1
}
