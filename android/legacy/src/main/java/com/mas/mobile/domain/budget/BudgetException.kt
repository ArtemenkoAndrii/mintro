package com.mas.mobile.domain.budget

class BudgetException : Exception {
    constructor(message: String): super(message)
    constructor(message: String, ex: Exception?): super(message, ex)
}