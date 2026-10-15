package com.mas.mobile.presentation.viewmodel

class ItemNotFoundException(message: String) : RuntimeException(message)

class ActionNotSupportedException(message: String) : RuntimeException(message)