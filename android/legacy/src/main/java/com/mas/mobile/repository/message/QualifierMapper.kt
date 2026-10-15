package com.mas.mobile.repository.message

import com.mas.mobile.domain.message.Qualifier
import com.mas.mobile.domain.message.QualifierId
import com.mas.mobile.repository.db.entity.Qualifier.Companion.BLACKLIST
import com.mas.mobile.repository.db.entity.Qualifier.Companion.CATCH
import com.mas.mobile.repository.db.entity.Qualifier.Companion.SKIP
import com.mas.mobile.repository.db.entity.Qualifier as QualifierData

fun QualifierData.toModel() =
    Qualifier(
        id = QualifierId(id),
        value = name,
        type = type.toQualifierType()
    )

fun Short.toQualifierType() =
    when(this) {
        CATCH -> Qualifier.Type.CATCH
        SKIP -> Qualifier.Type.SKIP
        BLACKLIST -> Qualifier.Type.BLACKLIST
        else -> Qualifier.Type.SKIP
    }

fun Qualifier.toDto() =
    QualifierData(
        id = this.id.value,
        name = this.value,
        type = type.toDTOType()
    )

fun Qualifier.Type.toDTOType() =
    when(this) {
        Qualifier.Type.CATCH -> CATCH
        Qualifier.Type.SKIP -> SKIP
        Qualifier.Type.BLACKLIST -> BLACKLIST
    }
