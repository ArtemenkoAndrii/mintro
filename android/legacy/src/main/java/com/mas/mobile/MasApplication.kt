package com.mas.mobile

import android.app.Application
import android.content.Context
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader
import com.maltaisn.iconpack.defaultpack.createDefaultIconPack
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.CategoryRepository
import com.mas.mobile.domain.budget.ExchangeRepository
import com.mas.mobile.domain.budget.ExpenditureRepository
import com.mas.mobile.domain.budget.SpendingRepository
import com.mas.mobile.domain.message.MessageAnalyzer
import com.mas.mobile.domain.message.MessageRepository
import com.mas.mobile.domain.message.MessageService
import com.mas.mobile.domain.message.MessageTemplateRepository
import com.mas.mobile.domain.message.QualifierRepository
import com.mas.mobile.domain.settings.DeferrableActionRepository
import com.mas.mobile.domain.settings.SettingsRepository
import com.mas.mobile.presentation.activity.IconPickerActivity
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.presentation.activity.PolicyActivity
import com.mas.mobile.presentation.activity.fragment.CommonFragment
import com.mas.mobile.presentation.viewmodel.BudgetListViewModel
import com.mas.mobile.presentation.viewmodel.BudgetViewModel
import com.mas.mobile.presentation.viewmodel.CategoryListViewModel
import com.mas.mobile.presentation.viewmodel.CategoryViewModel
import com.mas.mobile.presentation.viewmodel.ChartViewModel
import com.mas.mobile.presentation.viewmodel.ExpenditureListViewModel
import com.mas.mobile.presentation.viewmodel.ExpenditureViewModel
import com.mas.mobile.presentation.viewmodel.MessageListViewModel
import com.mas.mobile.presentation.viewmodel.MessageTemplateListViewModel
import com.mas.mobile.presentation.viewmodel.MessageTemplateViewModel
import com.mas.mobile.presentation.viewmodel.QualifierListViewModel
import com.mas.mobile.presentation.viewmodel.SettingsViewModel
import com.mas.mobile.presentation.viewmodel.SpendingListViewModel
import com.mas.mobile.presentation.viewmodel.SpendingViewModel
import com.mas.mobile.repository.BudgetRepositoryImpl
import com.mas.mobile.repository.budget.CategoryRepositoryImpl
import com.mas.mobile.repository.budget.ExpenditureRepositoryImpl
import com.mas.mobile.repository.budget.FreeCurrencyAPIRepositoryImpl
import com.mas.mobile.repository.budget.SpendingRepositoryImpl
import com.mas.mobile.repository.db.config.AppDatabase
import com.mas.mobile.repository.message.MessageRepositoryImpl
import com.mas.mobile.repository.message.MessageTemplateRepositoryImpl
import com.mas.mobile.repository.message.QualifierRepositoryImpl
import com.mas.mobile.repository.settings.DeferrableActionRepositoryImpl
import com.mas.mobile.repository.settings.SettingsRepositoryImpl
import com.mas.mobile.service.AppUpdateCheckWorker
import com.mas.mobile.service.CoroutineService
import com.mas.mobile.service.DateListener
import com.mas.mobile.service.NotificationListener
import com.mas.mobile.service.NotificationService
import com.mas.mobile.service.ResourceService
import com.mas.mobile.service.ScheduledSpendingWorker
import com.mas.mobile.service.SmsListener
import com.mas.mobile.service.TaskService
import com.mas.mobile.service.ai.GPTMessageAnalyzer
import com.mas.mobile.service.ai.GptChatApiClient
import com.mas.mobile.service.ai.GptChatConnector
import com.mas.mobile.util.FirebaseEventLogger
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

class MasApplication: Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent
            .builder()
            .context(this)
            .build()
    }

    companion object {
        const val HOME_PAGE = "https://github.com/ArtemenkoAndrii/MasHome/wiki/What-Mas-Money-is%3F"
    }
}

val Context.appComponent: AppComponent
    get() = when(this) {
        is MasApplication -> appComponent
        else -> this.applicationContext.appComponent
    }

@Component(modules = [AppModule::class, PersistenceModule::class, ExternalServicesModule::class])
@Singleton
interface AppComponent {
    fun messageService(): MessageService
    fun db(): AppDatabase

    fun messageListViewModel(): MessageListViewModel.Factory
    fun messageTemplateListViewModel(): MessageTemplateListViewModel.Factory
    fun messageTemplateViewModel(): MessageTemplateViewModel.Factory
    fun budgetViewModel(): BudgetViewModel.Factory
    fun budgetListViewModel(): BudgetListViewModel.Factory

    fun expenditureViewModel(): ExpenditureViewModel.Factory
    fun expenditureListViewModel(): ExpenditureListViewModel.Factory

    fun spendingViewModel(): SpendingViewModel.Factory
    fun spendingListViewModel(): SpendingListViewModel.Factory
    fun qualifierListViewModel(): QualifierListViewModel.Factory
    fun settingsModel(): SettingsViewModel.Factory
    fun chartViewModelModel(): ChartViewModel.Factory
    fun categoryViewModelModel(): CategoryViewModel.Factory
    fun categoryListViewModelModel(): CategoryListViewModel.Factory

    fun injectSmsListener(smsListener: SmsListener)
    fun injectDateListener(dateListener: DateListener)
    fun injectNotificationListener(notificationListener: NotificationListener)
    fun injectCommonFragment(wrapper: CommonFragment.CommonFragmentWrapper)
    fun injectAppUpdateCheckWorker(worker: AppUpdateCheckWorker)
    fun injectScheduledSpendingWorker(worker: ScheduledSpendingWorker)

    fun injectMainActivity(mainActivity: MainActivity)
    fun injectPolicyActivity(policyActivity: PolicyActivity)
    fun injectIconPickerActivity(iconPickerActivity: IconPickerActivity)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder
        fun persistenceModule(module: PersistenceModule): Builder
        fun externalServicesModule(module: ExternalServicesModule): Builder
        fun build(): AppComponent
    }
}

@Module
class AppModule {
    @Provides
    @Singleton
    fun resolveBudgetRepository(db: AppDatabase): BudgetRepository {
        return BudgetRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveExpenditureRepository(db: AppDatabase): ExpenditureRepository {
        return ExpenditureRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveSpendingRepository(db: AppDatabase): SpendingRepository {
        return SpendingRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveMessageRepository(db: AppDatabase): MessageRepository {
        return MessageRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveMessageTemplateRepository(db: AppDatabase): MessageTemplateRepository {
        return MessageTemplateRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveSettingsRepository(db: AppDatabase): SettingsRepository {
        return SettingsRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveQualifierRepository(db: AppDatabase): QualifierRepository {
        return QualifierRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveDeferredActionRepositoryImpl(db: AppDatabase): DeferrableActionRepository {
        return DeferrableActionRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveCategoryRepositoryImpl(db: AppDatabase): CategoryRepository {
        return CategoryRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun resolveAnalytics(): EventLogger {
        return FirebaseEventLogger()
    }

    @Provides
    @Singleton
    fun resolveNotificationService(context: Context, resourceService: ResourceService): NotificationService {
        return NotificationService(context, resourceService)
    }

    @Provides
    @Singleton
    fun resolveIconPack(context: Context): IconPack {
        val loader = IconPackLoader(context)
        val pack = createDefaultIconPack(loader)
        pack.loadDrawables(loader.drawableLoader)
        return pack
    }
}

// Persistence lives in its own module, open for instrumented tests to subclass and
// swap the real, file-backed AppDatabase for an in-memory one, without touching AppModule.
@Module
open class PersistenceModule {
    @Provides
    @Singleton
    open fun providesDb(context: Context): AppDatabase = AppDatabase.getInstance(context)
}

// Network/threading collaborators live in their own module, open for instrumented tests
// to subclass and swap for deterministic fakes (no live GPT/currency-API calls, no real
// background dispatcher), without touching AppModule.
@Module
open class ExternalServicesModule {
    @Provides
    @Singleton
    open fun resolveExchangeRepository(): ExchangeRepository {
        return FreeCurrencyAPIRepositoryImpl()
    }

    @Provides
    @Singleton
    open fun resolveGptChatService(): GptChatConnector =
        GptChatApiClient().gptChatConnector

    @Provides
    @Singleton
    open fun resolveMessageProcessor(gptChatConnector: GptChatConnector): MessageAnalyzer =
        GPTMessageAnalyzer(gptChatConnector)

    @Provides
    @Singleton
    open fun resolveTaskService(coroutineService: CoroutineService): TaskService {
        return coroutineService
    }
}