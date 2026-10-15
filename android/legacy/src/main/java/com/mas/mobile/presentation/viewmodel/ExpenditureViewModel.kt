package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mas.mobile.domain.Repository
import com.mas.mobile.domain.budget.*
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.presentation.viewmodel.validator.FieldValidator
import com.mas.mobile.presentation.viewmodel.validator.Validator
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ExpenditureViewModel @AssistedInject constructor(
    coroutineService: CoroutineService,
    private val budgetService: BudgetService,
    private val fieldValidator: FieldValidator,
    @Assisted("expenditureId") private val expenditureId: Int,
    @Assisted("budgetId") private val budgetId: Int,
    @Assisted private val action: Action,
) : ItemViewModel<Expenditure>(coroutineService, ExpenditureRepositoryAdapter(budgetService, budgetId)) {
    override val model: Expenditure = loadModel()

    private var originalName = ""

    val name = MutableLiveData<String>()
    val nameError = MutableLiveData(Validator.NO_ERRORS)
    val plan = MutableLiveData<Double>()
    val fact = MutableLiveData<Double>()
    val comment = MutableLiveData<String>()

    val availableExpenditures = budgetService
        .expenditureRepository
        .getExpenditureNames(true, 20)
        .map { it.value }
        .toList()

    init {
        initProperties(model)
    }

    private fun loadBudget(): Budget =
        if (budgetId > 0) {
            budgetService.budgetRepository.getBudget(budgetId)
        } else {
            budgetService.getActiveBudget()
        }
        ?: throw ItemNotFoundException("Budget not found id=$budgetId")

    private fun loadModel() =
        when(action) {
            Action.ADD -> budgetService.expenditureRepository.create().also { enableEditing() }
            Action.VIEW -> loadBudget().getExpenditure(ExpenditureId(expenditureId))
            Action.EDIT -> loadBudget().getExpenditure(ExpenditureId(expenditureId)).also { enableEditing() }
            Action.CLONE -> {
                val origin = loadBudget().getExpenditure(ExpenditureId(expenditureId))
                enableEditing()
                budgetService.expenditureRepository.create().also {
                    if (origin != null) {
                        it.name = origin.name
                        it.plan = origin.plan
                        it.comment = origin.comment
                    }
                }
            }
            else -> throw ActionNotSupportedException("BudgetViewModel does not support $action action")
        } ?: throw ItemNotFoundException("Item not found id=$budgetId")

    private fun initProperties(item: Expenditure) {
        originalName = item.name
        name.value = item.name
        name.observeForever {
            item.name = it.trim()
            validateOnChange(nameError) {
                fieldValidator.minLength(item.name, EXPENDITURE_MIN_LENGTH)
            }
        }

        plan.value = item.plan
        plan.observeForever{
            item.plan = it
        }

        fact.value = item.fact
        fact.observeForever{
            item.fact = it
        }

        comment.value = item.comment
        comment.observeForever{
            item.comment = it
        }

        validateOnSave(nameError) {
            fieldValidator.alreadyExists(ifExpenditureExists(name.value))
        }
    }

    private fun ifExpenditureExists(name: String?): Boolean {
        return if (name != originalName || action == Action.ADD || action == Action.CLONE) {
            budgetService.loadBudgetOrGetActive(BudgetId(budgetId)).findExpenditure(name ?: "") != null
        } else {
            false
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("expenditureId") expenditureId: Int,
                   @Assisted("budgetId") budgetId: Int,
                   action: Action): ExpenditureViewModel
    }

    companion object {
        const val EXPENDITURE_MIN_LENGTH = 3
    }
}

class ExpenditureRepositoryAdapter(val service: BudgetService, val budgetId: Int) : Repository<Expenditure> {
    private val budget = service.loadBudgetOrGetActive(BudgetId(budgetId))

    override suspend fun save(item: Expenditure) {
        budget.addExpenditure(item)
        service.budgetRepository.save(budget)
    }

    override suspend fun remove(item: Expenditure) {
        budget.removeExpenditure(item)
        service.budgetRepository.save(budget)
    }
}
