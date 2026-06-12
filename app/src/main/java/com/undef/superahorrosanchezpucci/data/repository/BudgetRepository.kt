package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.PresupuestoEntity
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TipoPresupuesto
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.BudgetResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.CreateBudgetRequest
import com.undef.superahorrosanchezpucci.data.remote.dto.UpdateBudgetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BudgetRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    suspend fun getBudgets(groupId: String, isIndividual: Boolean): List<Presupuesto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBudgets(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val budgets = response.body()?.data ?: emptyList()
                val models = budgets.mapIndexed { index, b -> b.toModel(isIndividual, index == 0) }
                if (models.isNotEmpty()) {
                    appDao.clearPresupuestos()
                    appDao.insertPresupuestos(models.map { it.toEntity() })
                }
                return@withContext models
            }
        } catch (e: Exception) {
            Log.e("BudgetRepo", "Error fetching budgets", e)
        }
        appDao.getPresupuestos().map { it.toModel() }
    }

    suspend fun createDefaultBudget(groupId: String) = withContext(Dispatchers.IO) {
        try {
            val categories = apiService.getCategories().body()?.data
            val catId = categories?.firstOrNull()?.id ?: ""
            apiService.createBudget(CreateBudgetRequest(
                groupId = groupId,
                name = "Presupuesto Mensual",
                totalAmount = 1.0,
                period = "MONTHLY",
                startDate = LocalDateTime.now().toString(),
                items = emptyList()
            ))
        } catch (_: Exception) {}
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

    // Mappings
    private fun BudgetResponse.toModel(isIndividual: Boolean, isActive: Boolean) = Presupuesto(
        id = id,
        tipo = if (isIndividual) TipoPresupuesto.INDIVIDUAL else TipoPresupuesto.FAMILIAR,
        nombre = name,
        montoTotal = totalAmount.toInt(),
        montoDisponible = 0,
        fechaInicio = parseDate(startDate),
        fechaFin = endDate?.let { parseDate(it) },
        activo = isActive
    )

    private fun PresupuestoEntity.toModel() = Presupuesto(
        id, if (tipo == "FAMILIAR") TipoPresupuesto.FAMILIAR else TipoPresupuesto.INDIVIDUAL,
        nombre, montoTotal, montoDisponible, fechaInicio, fechaFin, activo
    )

    private fun Presupuesto.toEntity() = PresupuestoEntity(
        id, if (tipo == TipoPresupuesto.FAMILIAR) "FAMILIAR" else "INDIVIDUAL",
        nombre, montoTotal, montoDisponible, fechaInicio, fechaFin, activo
    )

    private fun parseDate(date: String): Long = try {
        LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { System.currentTimeMillis() }
}
