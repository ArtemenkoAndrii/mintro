package com.mas.mobile.repository

class RepositoryException : Exception {
    constructor(message: String): super(message)
    constructor(message: String, ex: Exception?): super(message, ex)
}