package com.mas.mobile.presentation.viewmodel.validator

import java.time.LocalDate

class Validator<T>(private val value: T) {
    var message: String = ""

    fun minLength(min: Int): Validator<T> {
        val length = value.toString().length
        if (length < min) {
            message = "At least $min characters"
        }
        return this
    }

    fun onlyFuture(): Validator<T> {
        if (value is LocalDate && value.compareTo(LocalDate.now()) < 0) {
            message = "Future tense only"
        }
        return this
    }

    fun valueChosen(): Validator<T> {
        if (value is Int) {
            if (value <= 0) {
                message = "Choose value from the list"
            }
        }
        return this
    }


    companion object {
        const val NO_ERRORS: String = ""
    }

}