package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.presentation.viewmodel.validator.FieldValidator
import com.mas.mobile.presentation.viewmodel.validator.Validator
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.Currency

class BudgetViewModel @AssistedInject constructor(
    coroutineService: CoroutineService,
    private val fieldValidator: FieldValidator,
    private val budgetRepository: BudgetRepository,
    private val budgetService: BudgetService,
    @Assisted private val action: Action,
    @Assisted private val budgetId: Int
) : ItemViewModel<Budget>(coroutineService, budgetRepository) {
    override val model: Budget = loadModel()

    var startsOnValue: LocalDate = LocalDate.now()
    var lastDayAtValue: LocalDate = LocalDate.now()

    val name = MutableLiveData<String>()
    val nameError = MutableLiveData(Validator.NO_ERRORS)
    val plan = MutableLiveData<Double>()
    val fact = MutableLiveData<Double>()
    val startsOn = MutableLiveData<LocalDate>()
    val startsOnError = MutableLiveData(Validator.NO_ERRORS)
    val lastDayAt = MutableLiveData<LocalDate>()
    val lastDayAtError = MutableLiveData(Validator.NO_ERRORS)
    val currency = MutableLiveData<Currency>()
    val comment = MutableLiveData<String?>()

    init {
        initProperties(model)
    }

    private fun loadModel() =
        when(action) {
            Action.ADD -> budgetService.createNext()
            Action.VIEW -> budgetRepository.getBudget(budgetId)
            Action.EDIT -> budgetRepository.getBudget(budgetId).also { enableEditing() }
            else -> throw ActionNotSupportedException("BudgetViewModel does not support $action action")
        } ?: throw ItemNotFoundException("Item not found id=$budgetId")

    private fun initProperties(item: Budget) {
        name.value = item.name
        name.observeForever {
            item.name = it
            validateOnChange(nameError) {
                fieldValidator.minLength(name.value, EXPENDITURE_MIN_LENGTH)
            }
        }

        plan.value = item.plan
        plan.observeForever {
            item.plan = it
        }

        fact.value = item.fact
        fact.observeForever {
            item.fact = it
        }

        startsOn.value = item.startsOn
        startsOn.observeForever {
            item.startsOn = it
            startsOnValue = it
        }

        lastDayAt.value = item.lastDayAt
        lastDayAt.observeForever {
            item.lastDayAt = it
            lastDayAtValue = it
        }

        currency.value = item.currency
        currency.observeForever {
            item.currency = it
        }

        comment.value = item.comment
        comment.observeForever {
            item.comment = it
        }
    }

    fun isChangeable() =
        budgetRepository.getLast()?.let { it.id.value == budgetId } ?: true

    @AssistedFactory
    interface Factory {
        fun create(action: Action, budget: Int): BudgetViewModel
    }

    companion object {
        const val EXPENDITURE_MIN_LENGTH = 3
    }
}
