package com.mas.mobile.repository.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.mas.mobile.domain.message.Qualifier
import com.mas.mobile.domain.message.QualifierId
import com.mas.mobile.domain.message.QualifierLiveData
import com.mas.mobile.domain.message.QualifierRepository
import com.mas.mobile.repository.db.config.AppDatabase
import javax.inject.Singleton

@Singleton
class QualifierRepositoryImpl(val db: AppDatabase) : QualifierRepository {
    private val dao = db.qualifierDAO()

    override val live: QualifierLiveData = object : QualifierLiveData {
        override fun getQualifiers(type: Qualifier.Type): LiveData<List<Qualifier>> =
            dao.getQualifiersLive(type.toDTOType()).map { qualifiers ->
                qualifiers.map { it.toModel() }.toList()
            }
    }

    override fun getQualifiers(type: Qualifier.Type): List<Qualifier> =
        dao.getQualifiers(type.toDTOType()).map { it.toModel() }

    override fun create(): Qualifier =
        Qualifier(
            id = QualifierId(db.idGeneratorDAO().generateId().toInt()),
            type = Qualifier.Type.SKIP,
            value = ""
        )

    override suspend fun save(item: Qualifier) {
        dao.upsert(item.toDto())
    }

    override suspend fun remove(item: Qualifier) {
        dao.delete(item.toDto())
    }
}