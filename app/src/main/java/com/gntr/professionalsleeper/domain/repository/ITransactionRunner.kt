package com.gntr.professionalsleeper.domain.repository

interface ITransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}