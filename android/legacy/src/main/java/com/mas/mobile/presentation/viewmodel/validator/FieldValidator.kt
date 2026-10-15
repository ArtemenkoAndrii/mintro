package com.mas.mobile.presentation.viewmodel.validator

import android.content.Context
import com.mas.mobile.R
import java.time.LocalDate
import javax.inject.Inject

class FieldValidator @Inject constructor(
    private val context: Context
) {
    fun minLength(value: String?, minLength: Int) =
        if (value == null || value.trim().length < minLength) {
            getValidationMessage(R.string.validator_min_length, minLength)
        } else {
            NO_ERRORS
        }

    fun minLength(list: List<String>, minLength: Int) =
        list.firstOrNull { it.length < minLength }
            ?.errorMessage(R.string.validator_min_length_in_list, minLength)
            ?: NO_ERRORS

    fun onlyFuture(value: LocalDate?) =
        if (value == null || value < LocalDate.now()) {
            getValidationMessage(R.string.validator_only_future)
        } else {
            NO_ERRORS
        }

    fun alreadyExists(value: Boolean) =
        if (value) {
            getValidationMessage(R.string.validator_already_exists)
        } else {
            NO_ERRORS
        }

    private fun getValidationMessage(resId: Int, vararg args: Any?): String {
        val mask = this.context.getString(resId)
        return String.format(mask, *args)
    }

    private fun String.errorMessage(resId: Int, vararg args: Any?) =
        getValidationMessage(resId, this, *args)

    companion object {
        const val NO_ERRORS = ""
    }
}