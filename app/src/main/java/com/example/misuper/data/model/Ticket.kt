package com.example.misuper.data.model

data class Ticket(
    val id: String,
    val supermercado: String,
    val direccion: String,
    val fechaHora: Long,
    val total: Int,
    val metodoPago: MetodoPago,
    val imagenPath: String,
    val presupuestoId: String, // Vincular al presupuesto (familiar o individual)
    val productos: List<TicketProducto> = emptyList()
)

data class TicketProducto(
    val nombre: String,
    val precio: Int,
    val cantidad: Int = 1
)

enum class MetodoPago {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA
}