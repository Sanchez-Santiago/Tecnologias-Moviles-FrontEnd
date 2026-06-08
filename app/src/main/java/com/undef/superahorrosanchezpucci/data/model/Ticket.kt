package com.undef.superahorrosanchezpucci.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ticket(
    val id: String,
    val supermercado: String,
    val direccion: String,
    val fechaHora: Long,
    val total: Int,
    val metodoPago: MetodoPago,
    val imagenPath: String,
    val presupuestoId: String,
    val productos: List<TicketProducto> = emptyList()
) : Parcelable

@Parcelize
data class TicketProducto(
    val nombre: String,
    val precio: Int,
    val cantidad: Int = 1
) : Parcelable

data class TicketImageAnalysis(
    val storeName: String?,
    val purchaseDate: String?,
    val total: Int?,
    val products: List<TicketProducto>
)

@Parcelize
enum class MetodoPago : Parcelable {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA
}
