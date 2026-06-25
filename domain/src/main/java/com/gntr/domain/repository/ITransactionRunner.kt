package com.gntr.domain.repository

interface ITransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}