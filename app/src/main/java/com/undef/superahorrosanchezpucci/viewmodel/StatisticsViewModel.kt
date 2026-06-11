package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.remote.dto.GroupDetailResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.MonthlySummary
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByCategory
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByImportance
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByStore
import com.undef.superahorrosanchezpucci.data.remote.dto.StoreFrequency
import com.undef.superahorrosanchezpucci.data.remote.dto.MostPurchasedProduct
import com.undef.superahorrosanchezpucci.data.remote.dto.MemberSpending
import kotlinx.coroutines.flow.StateFlow

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val spendingByCategory: StateFlow<List<SpendingByCategory>> = store.spendingByCategory
    val spendingByStore: StateFlow<List<SpendingByStore>> = store.spendingByStore
    val spendingByImportance: StateFlow<List<SpendingByImportance>> = store.spendingByImportance
    val monthlySummary: StateFlow<List<MonthlySummary>> = store.monthlySummary
    val storeFrequency: StateFlow<List<StoreFrequency>> = store.storeFrequency
    val mostPurchasedProducts: StateFlow<List<MostPurchasedProduct>> = store.mostPurchasedProducts
    val memberSpending: StateFlow<List<MemberSpending>> = store.memberSpending
    val grupos: StateFlow<List<GroupDetailResponse>> = store.grupos
    val isLoading: StateFlow<Boolean> = store.isLoading

    fun loadStats(grupoId: String) = store.loadStats(grupoId)
}
