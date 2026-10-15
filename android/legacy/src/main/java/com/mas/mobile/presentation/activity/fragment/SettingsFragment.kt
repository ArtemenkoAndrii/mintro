package com.mas.mobile.presentation.activity.fragment

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.mas.mobile.MasApplication
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.SettingsFragmentBinding
import com.mas.mobile.presentation.activity.converter.TextDrawable
import com.mas.mobile.presentation.viewmodel.SettingsViewModel
import com.mas.mobile.service.NotificationListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class SettingsFragment : CommonFragment() {
    private lateinit var binding: SettingsFragmentBinding
    private val args: SettingsFragmentArgs by navArgs()
    private var permissionLauncher = this.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        settingsViewModel.captureSms.value = it
    }
    private var exportLauncher = this.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { settingsViewModel.exportBudgets(it) }
    }
    private var importLauncher = this.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { settingsViewModel.importBudgets(it) }
    }

    private val settingsViewModel: SettingsViewModel by lazyViewModel {
        this.requireContext().appComponent.settingsModel().create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.settings_fragment, container, false)

        binding = SettingsFragmentBinding.bind(layout)
        binding.settings = settingsViewModel
        binding.lifecycleOwner = this

        settingsViewModel.period.observeForever { value ->
            binding.settingsPeriodLayout.setOnClickListener {
                showOptionsPicker(
                    getResourceService().messageSettingsPeriod(),
                    settingsViewModel.periodMap.values.toList(),
                    value
                ) { result ->
                    settingsViewModel.period.value = result
                }
            }
        }

        settingsViewModel.startDayOfMonth.observeForever { day ->
            binding.settingsStartDayOfMonth.setOnClickListener {
                showListPicker(
                    getResourceService().messageSettingsStartDay(),
                    settingsViewModel.availableDaysOfMonth,
                    day
                ) { value ->
                    settingsViewModel.startDayOfMonth.value = value
                }
            }
        }

        settingsViewModel.startDayOfWeek.observeForever { day ->
            binding.settingsStartDayOfWeek.setOnClickListener {
                showListPicker(
                    getResourceService().messageSettingsStartDay(),
                    settingsViewModel.availableDaysOfWeek,
                    day
                ) { value ->
                    settingsViewModel.startDayOfWeek.value = value
                }
            }
        }

        binding.settingsCaptureNotificationsLayout.setOnClickListener {
            binding.settingsCaptureNotifications.performClick()
        }

        binding.settingsCaptureSmsLayout.setOnClickListener {
            binding.settingsCaptureSms.performClick()
        }

        binding.settingsWhatIsLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(MasApplication.HOME_PAGE)
            startActivity(intent)
        }

        binding.settingsMessageTemplatesLayout.setOnClickListener {
            go(SettingsFragmentDirections.actionToMessageTemplateList())
        }

        with(binding.settingsCurrencyLayout) {
            setOnClickListener {
                showCurrencyPicker { currency ->
                    settingsViewModel.currency.value = currency
                    clearFocus()
                }
            }
        }

        settingsViewModel.onRequestSMSPermissions { requestSMSPermissions() }
        settingsViewModel.onRequestNotificationPermissions { showNotificationSettings() }

        binding.settingsBackupExportLayout.setOnClickListener {
            exportLauncher.launch(backupFileName())
        }

        binding.settingsBackupImportLayout.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }

        settingsViewModel.backupInProgress.observeForever { inProgress ->
            binding.settingsBackupProgressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
            binding.settingsBackupExportLayout.isEnabled = !inProgress
            binding.settingsBackupImportLayout.isEnabled = !inProgress
        }

        settingsViewModel.onBackupCompleted { message -> showInfoDialog(message) {} }
        settingsViewModel.onBackupFailed { message -> showInfoDialog(message) {} }

        return layout
    }

    private fun backupFileName() =
        "mashome-backup-${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.json"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.guided) {
            blink(binding.settingsCaptureNotifications) {
                blink(binding.settingsCaptureSms)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    private fun blink(switch: SwitchCompat, after: () -> Unit = {}) {
        val originalDrawable = switch.background as? RippleDrawable
        val redRippleDrawable = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)),
            null,
            null)

        switch.background = redRippleDrawable
        redRippleDrawable.state = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)

        switch.postDelayed(
            {
                switch.background = originalDrawable
                after()
            },
            850
        )
    }

    private fun requestSMSPermissions() {
        permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
    }

    private fun showNotificationSettings() {
        val component = ComponentName(this.requireContext(), NotificationListener::class.java)
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).also {
            it.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            this.requireContext().startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            showInfoDialog(getResourceService().messageAllowNotifications()) {}
        }
    }

    private fun showListPicker(title: String, list: List<String>, default: String?, result: (String) -> Unit) {
        val numberPicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = list.size - 1
            displayedValues = list.toTypedArray()
            value = list.indexOf(default).takeIf { it >= 0 } ?: 0
        }

        val builder = AlertDialog.Builder(requireContext()).also {
            it.setTitle(title)
            it.setView(numberPicker)
            it.setPositiveButton(getResourceService().dialogConfirmationOk()) { _, _ ->
                result(list[numberPicker.value])
            }
            it.setNegativeButton(getResourceService().dialogConfirmationCancel()) { dialog, _ ->
                dialog.dismiss()
            }
        }

        builder.create().show()
    }
}

