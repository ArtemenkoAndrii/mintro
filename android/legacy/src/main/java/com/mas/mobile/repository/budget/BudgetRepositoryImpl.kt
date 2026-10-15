package com.mas.mobile.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.mas.mobile.domain.budget.*
import com.mas.mobile.repository.budget.BudgetMapper
import com.mas.mobile.repository.budget.NullableTransformations
import com.mas.mobile.repository.budget.toDto
import com.mas.mobile.repository.budget.toModel
import com.mas.mobile.repository.db.config.AppDatabase
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.time.LocalDate
import java.util.concurrent.Callable
import javax.inject.Singleton
import com.mas.mobile.repository.db.entity.Budget as BudgetData

@Singleton
class BudgetRepositoryImpl(
    val db: AppDatabase
) : BudgetRepository {
    val dao = db.budgetDao()

    override val live: BudgetLiveData
        get() = BudgetLiveDataImpl(db)

    override fun createBudget(): Budget {
        val id = BudgetId(generateId().toInt())
        return Budget(id = id, lazyLoader = getLoader(db, id))
    }

    override fun getBudget(budgetId: Int): Budget? = dao.getById(budgetId)?.toModel()

    override fun getBudgetByName(name: String): Budget? = dao.getByName(name)?.toModel()

    override fun getBudgetBySpendingId(id: SpendingId): Budget? = dao.getBySpendingId(id.value)?.toModel()

    override fun getOnDate(date: LocalDate): Budget? = dao.getActiveOn(date)?.toModel()

    override fun getLast(): Budget? = dao.getLatestEndedOn(LocalDate.MAX)?.toModel()

    override fun getCompleted(): List<Budget> = dao.getCompleted().map { it.toModel() }

    override fun getAll(): List<Budget> = dao.getAll().map { it.toModel() }

    override fun generateId(): Long {
        return db.idGeneratorDAO().generateId()
    }

    override suspend fun save(item: Budget) {
        db.runInTransaction(Callable {
            runBlocking {
                dao.upsert(BudgetMapper.toDto(item))
                saveExpenditures(item.budgetDetails.expenditure)
                saveSpendings(item.budgetDetails.spending)
            }
        })
    }

    override suspend fun remove(item: Budget) = db.budgetDao().delete(item.toDto())

    private suspend fun saveExpenditures(expenditures: List<Expenditure>) {
        when(expenditures) {
            is TrackingList -> {
                expenditures.getDeleted().forEach {
                    db.expenditureDao().deleteExpenditureById(it)
                }

                expenditures.getChanged().forEach {
                    db.expenditureDao().upsert(it.toDto())
                }
            }
            else -> throw RepositoryException("Unsupportable expenditure collection: ${expenditures.javaClass}")
        }
    }

    private suspend fun saveSpendings(spendings: List<Spending>) {
        when(spendings) {
            is TrackingList -> {
                spendings.getDeleted().forEach {
                    db.spendingDao().deleteSpendingById(it)
                    db.spendingMessageDao().removeBindSpendingById(it)
                }

                spendings.getChanged().forEach {
                    db.spendingDao().upsert(it.toDto())
                }
            }
            else -> throw RepositoryException("Unsupportable spending collection: ${spendings.javaClass}")
        }
    }

    private fun BudgetData.toModel() = BudgetMapper.toModel(this, getLoader(db, BudgetId(this.id)))

    private fun Budget.toDto() = BudgetMapper.toDto(this)
}

class BudgetLiveDataImpl(val db: AppDatabase) : BudgetLiveData {
    val dao = db.budgetDao()

    override fun getBudget(budgetId: BudgetId): LiveData<Budget> =
        dao.getByIdLive(budgetId.value).toModel()

    override fun getBudgets(): LiveData<List<Budget>> =
        db.budgetDao().getAllLive().map { budgets ->
            budgets.map { it.toModel() }.toList()
        }

    private fun BudgetData.toModel() = BudgetMapper.toModel(this, getLoader(db, BudgetId(this.id)))

    private fun LiveData<BudgetData>.toModel() =
        NullableTransformations.map<BudgetData, Budget>(this) {
            it?.toModel()
        }
}

private fun getLoader(db: AppDatabase, budgetId: BudgetId) = lazy {
    val expenditures = db.expenditureDao()
        .getByBudgetId(budgetId.value)
        .map { it.data.toModel() }
        .toMutableList()
        .let { list ->
            TrackingList(list) { it.id.value }
        }

    val spendings = db.spendingDao()
        .getByBudgetId(budgetId.value)
        .map { it.data.toModel(it.expenditure.toModel()) }
        .toMutableList()
        .let { list ->
            TrackingList(list) { it.id.value }
        }

    BudgetDetails(expenditures, spendings)
}

private class TrackingList<T>(
    private val list : MutableList<T>,
    private val getId: (T) -> Int
) : MutableList<T> by list {
    private val origin = list.associate { getId(it) to getHash(it) }

    fun getDeleted() =

        origin.keys.filter { o -> !list.any { getId(it) == o } }.toSet()

    fun getChanged() =
        list.filter { origin[getId(it)] != getHash(it) }.toSet()

    private fun getHash(value: T): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(value.toString().toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}