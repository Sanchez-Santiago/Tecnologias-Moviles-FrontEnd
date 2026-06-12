package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.*
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.CreateBudgetRequest
import com.undef.superahorrosanchezpucci.data.remote.dto.UpdateBudgetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class BudgetRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    /**
     * Obtiene los presupuestos del grupo.
     * Estrategia: Cache -> API -> Update Cache.
     */
    suspend fun getBudgets(groupId: String, isIndividual: Boolean): List<Presupuesto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBudgets(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val budgets = response.body()?.data ?: emptyList()
                val models = budgets.mapIndexed { index, b -> b.toModel(isIndividual, index == 0) }
                
                if (models.isNotEmpty()) {
                    // Actualizar Room
                    appDao.insertPresupuestos(models.map { it.toEntity() })
                }
                return@withContext models
            }
        } catch (e: Exception) {
            Log.e("BudgetRepo", "Error fetching budgets from API", e)
        }

        // Fallback a Room
        appDao.getPresupuestos()
            .filter { it.groupId == groupId }
            .map { it.toModel() }
    }

    suspend fun createDefaultBudget(groupId: String) = withContext(Dispatchers.IO) {
        try {
            apiService.createBudget(CreateBudgetRequest(
                groupId = groupId,
                name = "Presupuesto Mensual",
                totalAmount = 1.0,
                period = "MONTHLY",
                startDate = LocalDateTime.now().toString(),
                items = emptyList()
            ))
        } catch (e: Exception) {
            Log.e("BudgetRepo", "Error creating default budget", e)
        }
    }

    suspend fun updateBudget(presupuesto: Presupuesto) = withContext(Dispatchers.IO) {
        try {
            if (!presupuesto.id.startsWith("presupuesto-")) {
                apiService.updateBudget(presupuesto.id, UpdateBudgetRequest(presupuesto.nombre, presupuesto.montoTotal.toDouble()))
            }
            appDao.insertPresupuestos(listOf(presupuesto.toEntity()))
        } catch (_: Exception) {}
    }

    suspend fun activateBudget(id: String) = withContext(Dispatchers.IO) {
        try {
            if (!id.startsWith("presupuesto-")) apiService.activateBudget(id)
        } catch (_: Exception) {}
    }

    fun calculateAvailable(presupuestos: List<Presupuesto>, tickets: List<Ticket>): List<Presupuesto> {
        return presupuestos.map { p ->
            val spent = tickets.filter { it.presupuestoId == p.id }.sumOf { it.total }
            p.copy(montoDisponible = maxOf(0, p.montoTotal - spent))
        }
    }
}
