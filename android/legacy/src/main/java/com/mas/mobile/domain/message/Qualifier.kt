package com.mas.mobile.domain.message

data class Qualifier(
    val id: QualifierId,
    var type: Type,
    var value: String
) {
    enum class Type {
        CATCH, SKIP, BLACKLIST
    }
}

@JvmInline
value class QualifierId(val value: Int)
