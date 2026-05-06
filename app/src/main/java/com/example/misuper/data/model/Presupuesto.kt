package com.example.misuper.data.model

data class Presupuesto(
    val id: String,
    val tipo: TipoPresupuesto,
    val nombre: String, // Ej: "Familiar", "Juan"
    val montoTotal: Int,
    val montoDisponible: Int,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val activo: Boolean
)

enum class TipoPresupuesto {
    FAMILIAR,
    INDIVIDUAL
}