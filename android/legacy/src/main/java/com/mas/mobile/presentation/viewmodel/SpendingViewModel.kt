package com.mas.mobile.presentation.viewmodel

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mas.mobile.domain.Repository
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.analytics.ScheduledModified
import com.mas.mobile.domain.analytics.SpendingModified
import com.mas.mobile.domain.budget.*
import com.mas.mobile.domain.message.*
import com.mas.mobile.presentation.viewmodel.ExpenditureViewModel.Companion.EXPENDITURE_MIN_LENGTH
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.presentation.viewmodel.validator.FieldValidator
import com.mas.mobile.presentation.viewmodel.validator.Validator
import com.mas.mobile.service.CoroutineService
import com.mas.mobile.service.ResourceService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Currency

class SpendingViewModel @AssistedInject constructor(
    coroutineService: CoroutineService,
    resourceService: ResourceService,
    expenditureRepository: ExpenditureRepository,
    private val budgetService: BudgetService,
    private val spendingRepository: SpendingRepository,
    private val fieldValidator: FieldValidator,
    private val messageService: MessageService,
    private val categoryService: CategoryService,
    private val eventLogger: EventLogger,
    @Assisted private val action: Action,
    @Assisted("spendingId") private val pSpendingId: Int,
    @Assisted("expenditureId") private val pExpenditureId: Int,
    @Assisted("budgetId") private val pBudgetId: Int,
    @Assisted("messageId") private val pMessageId: Int,
) : ItemViewModel<Spending>(coroutineService, SpendingRepositoryAdapter(budgetService, pBudgetId, pSpendingId)) {
    private var merchantToSave: Merchant? = null

    val budget: Budget = loadBudget()
    override val model: Spending = loadModel()

    val discoverMode = MutableLiveData<Boolean>()
    var message: Message? = loadMessage()

    var comment = MutableLiveData<String>()
    var commentError = MutableLiveData(Validator.NO_ERRORS)
    var date = MutableLiveData<LocalDateTime>()
    var dateError = MutableLiveData(Validator.NO_ERRORS)
    var amount = MutableLiveData<Double>()
    var amountError = MutableLiveData(Validator.NO_ERRORS)
    var expenditureName = MutableLiveData<String>()
    var expenditureNameError = MutableLiveData(Validator.NO_ERRORS)
    var recurrence = MutableLiveData<String>()
    var scheduledDate = MutableLiveData<LocalDateTime?>()

    var exchangeAmount = MutableLiveData<Double>()
    var exchangeRate = MutableLiveData<Double>()
    var exchangeCurrency = MutableLiveData<Currency>()
    val exchangeVisibility = exchangeCurrency.map {
        if (model.exchangeInfo != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private var expenditureNameValue = ""

    val availableExpenditures = expenditureRepository
        .getExpenditureNames(true, 20)
        .map { it.value }
        .toList()

    val availabilityRecurrences = resourceService.constantRecurrenceMap()

    fun loadFromRecommended(handleResult: (success: Boolean) -> Unit) {
        val message = this.message ?: return handleResult(false)
        viewModelScope.launch {
            handleResult(promoteAndLoad(message))
        }
    }

    fun getCategory(): String = expenditureNameValue
    fun getMerchant(): String = this.merchantToSave?.value ?: ""
    fun hasUnsavedMerchant(): Boolean = this.merchantToSave != null && isValid()

    fun skipSavingMerchant() {
        this.merchantToSave = null
    }

    fun getCurrencyFromBudget() =
        if (budget.id.value == Budget.SCHEDULED_BUDGET_ID) {
            budgetService.getActiveBudget().currency
        } else {
            budget.currency
        }

    init {
        if (action != Action.VIEW) enableEditing()
        initProperties()
        initDefaultExpenditure(ExpenditureId(pExpenditureId))
        updateMerchant()
    }

    private fun loadBudget() =
        if (pSpendingId != -1) {
            budgetService.budgetRepository.getBudgetBySpendingId(SpendingId(pSpendingId))
                ?: budgetService.loadBudgetOrGetActive(BudgetId(pBudgetId))
        } else {
            budgetService.loadBudgetOrGetActive(BudgetId(pBudgetId))
        }

    private fun loadModel(): Spending =
        when(action) {
            Action.ADD -> spendingRepository.create()
            Action.VIEW, Action.EDIT -> spendingRepository.getSpending(SpendingId(pSpendingId))
            Action.CLONE -> {
                spendingRepository.create().also {
                    val origin = spendingRepository.getSpending(SpendingId(pSpendingId))
                    if (origin != null) {
                        it.amount = origin.amount
                        it.date = LocalDateTime.now()
                        it.comment = origin.comment
                        it.expenditure = findOrCreateExpenditure(origin.expenditure.name)
                        it.exchangeInfo = origin.exchangeInfo
                    }
                }
            }
            else -> throw ActionNotSupportedException("BudgetViewModel does not support $action action")
        } ?: throw ItemNotFoundException("Item not found id=$pBudgetId")

    private fun loadMessage(): Message? =
        if (pMessageId > 0) {
            messageService.repository.getById(MessageId(pMessageId))
        } else {
            messageService.repository.getBySpendingId(SpendingId(pSpendingId))
        }

    private fun initProperties() {
        comment.value = model.comment
        comment.observeForever {
            model.comment = it
        }

        date.value = model.date
        date.observeForever {
            model.date = it
            scheduledDate.value = model.scheduledDate
        }

        amount.value = model.amount
        amount.observeForever {
            model.amount = it
        }

        expenditureName.value = model.expenditure.name
        expenditureName.observeForever {
            expenditureNameValue = it.trim()
            validateOnChange(expenditureNameError) {
                fieldValidator.minLength(expenditureName.value, EXPENDITURE_MIN_LENGTH)
            }
        }

        message?.let {
            if (this.action == Action.ADD) {
                if (it.status is Message.Matched) {
                    amount.value = (it.status as Message.Matched).amount
                    comment.value = it.text
                } else {
                    discoverMode.value = true
                }
            }
        }

        exchangeCurrency.value = model.exchangeInfo?.currency ?: getCurrencyFromBudget()
        exchangeCurrency.observeForever {
            if (it != getCurrencyFromBudget()) {
                if (model.exchangeInfo == null) {
                    model.exchangeInfo = ExchangeInfo(
                        rawAmount = model.amount,
                        rate = budgetService.getRate(it),
                        currency = it
                    )
                } else {
                    model.exchangeInfo?.rate = budgetService.getRate(it)
                    model.exchangeInfo?.currency = it
                }
                model.exchange()
                exchangeRate.value = model.exchangeInfo?.rate ?: ZERO
                exchangeAmount.value = model.exchangeInfo?.rawAmount ?: ZERO
            } else {
                amount.value = model.exchangeInfo?.rawAmount ?: model.amount
                model.exchangeInfo = null
            }
        }

        exchangeAmount.value = model.exchangeInfo?.rawAmount ?: ZERO
        exchangeAmount.observeForever {
            model.exchangeInfo?.rawAmount = it
            model.exchange()
            amount.value = model.amount
        }

        exchangeRate.value = model.exchangeInfo?.rate ?: ZERO
        exchangeRate.observeForever {
            model.exchangeInfo?.rate = it
            model.exchange()
            exchangeAmount.value = model.exchangeInfo?.rawAmount ?: ZERO
        }

        recurrence.value = availabilityRecurrences[model.recurrence]
        recurrence.observeForever { value ->
            val key = availabilityRecurrences.filterValues { it == value }.keys.firstOrNull()
            if (key != null) {
                model.recurrence = key
                scheduledDate.value = model.scheduledDate
            }
        }
    }

    private suspend fun promoteAndLoad(message: Message): Boolean {
        val promotedMessage = messageService.promoteRecommendedMessage(message) ?: return false
        val amount = promotedMessage.getAmount() ?: return false
        val currency = messageService.getCurrency(promotedMessage) ?: return false

        this.discoverMode.value = false
        this.message = promotedMessage
        this.comment.value = message.text
        this.date.value = message.receivedAt
        this.amount.value = amount
        this.exchangeCurrency.value = currency
        this.expenditureName.value = ""

        updateMerchant()

        return true
    }

    private fun updateMerchant() {
        if (action == Action.ADD) {
            val merchant = this.message?.getMerchant() ?: return
            val category = categoryService.findCategoryByMerchant(merchant)
            if (category == null) {
                this.merchantToSave = merchant
            }
        }
    }

    override suspend fun doSave() {
        beforeSave()
        super.doSave()
        saveDependencies()
        logSaveEvent()
    }

    override suspend fun doRemove() {
        super.doRemove()
        logRemoveEvent()
    }

    private fun findOrCreateExpenditure(name: String) =
        budget.findExpenditure(name) ?: budgetService.createExpenditure(name)

    private fun initDefaultExpenditure(expenditureId: ExpenditureId) {
        if (action == Action.ADD && expenditureId.value > 0) {
            expenditureName.value = budget.getExpenditure(expenditureId)?.name ?: ""
        }
    }

    private fun beforeSave() {
        model.expenditure = findOrCreateExpenditure(expenditureNameValue)
    }

    private suspend fun saveDependencies() {
        message?.let {
            it.spendingId = model.id
            messageService.repository.save(it)
        }

        this.merchantToSave?.let {
            val category = categoryService.findCategoryByName(model.expenditure.name)
            if (category != null) {
                category.merchants.add(it)
                categoryService.repository.save(category)
            }
        }
    }

    private fun logSaveEvent() {
        val event = when {
            model.recurrence == Recurrence.Never && message != null -> SpendingModified(SpendingModified.Status.Save, true)
            model.recurrence == Recurrence.Never -> SpendingModified(SpendingModified.Status.Save)
            else -> ScheduledModified(ScheduledModified.Status.Save)
        }

        eventLogger.log(event)
    }

    private fun logRemoveEvent() {
        val event = when {
            model.recurrence == Recurrence.Never && message != null -> SpendingModified(SpendingModified.Status.Remove, true)
            model.recurrence == Recurrence.Never -> SpendingModified(SpendingModified.Status.Remove)
            else -> ScheduledModified(ScheduledModified.Status.Remove)
        }

        eventLogger.log(event)
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("spendingId") spendingId: Int,
                   @Assisted("expenditureId") expenditureId: Int,
                   @Assisted("budgetId") budgetId: Int,
                   @Assisted("messageId") messageId: Int,
                   action: Action): SpendingViewModel
    }

    companion object {
        private const val ZERO = 0.00
    }
}

class SpendingRepositoryAdapter(val service: BudgetService, private val pBudgetId: Int, private val pSpendingId: Int) : Repository<Spending> {
    private val budget: Budget = loadBudget()

    override suspend fun save(item: Spending) {
        if (!hasExpenditure(item.expenditure) ){
            budget.addExpenditure(item.expenditure)
        }

        budget.addSpending(item)
        service.budgetRepository.save(budget)
    }

    override suspend fun remove(item: Spending) {
        budget.removeSpending(item)
        service.budgetRepository.save(budget)
    }

    private fun hasExpenditure(expenditure: Expenditure) =
        budget.findExpenditure(expenditure.name) != null

    private fun loadBudget() =
        if (pSpendingId != -1) {
            service.budgetRepository.getBudgetBySpendingId(SpendingId(pSpendingId))
                ?: service.loadBudgetOrGetActive(BudgetId(pBudgetId))
        } else {
            service.loadBudgetOrGetActive(BudgetId(pBudgetId))
        }
}
