package com.undef.superahorrosanchezpucci.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Presupuesto(
    val id: String,
    val groupId: String = "",
    val tipo: TipoPresupuesto,
    val nombre: String,
    val montoTotal: Int,
    val montoDisponible: Int,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val activo: Boolean
) : Parcelable

enum class TipoPresupuesto {
    FAMILIAR,
    INDIVIDUAL
}