package com.gntr.domain.usecase

interface ISeedDatabaseUseCase {
    suspend operator fun invoke()
}