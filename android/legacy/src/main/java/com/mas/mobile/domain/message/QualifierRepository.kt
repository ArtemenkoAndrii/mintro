package com.mas.mobile.domain.message

import androidx.lifecycle.LiveData
import com.mas.mobile.domain.Repository

interface QualifierRepository : Repository<Qualifier> {
    val live: QualifierLiveData

    fun getQualifiers(type: Qualifier.Type): List<Qualifier>
    fun create(): Qualifier
}

interface QualifierLiveData {
    fun getQualifiers(type: Qualifier.Type): LiveData<List<Qualifier>>
}
