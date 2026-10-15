package com.mas.mobile.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import com.mas.mobile.R
import com.mas.mobile.appComponent
import javax.inject.Inject

class IconPickerActivity : AppCompatActivity(), IconDialog.Callback {
    @Inject
    override lateinit var iconDialogIconPack: IconPack

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.appComponent.injectIconPickerActivity(this)

        setContentView(R.layout.icon_picker_activity)

        val iconDialog = IconDialog.newInstance(IconDialogSettings())
        iconDialog.show(supportFragmentManager, TAG)
    }
    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        val iconId = icons[0].id
        val resultIntent = Intent().apply {
            putExtra(SELECTED_ICON_ID, iconId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onIconDialogCancelled() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    companion object {
        const val TAG = "icon_dialog"
        const val SELECTED_ICON_ID = "selected_icon_id"
    }
}