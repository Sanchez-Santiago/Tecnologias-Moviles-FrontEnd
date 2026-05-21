package com.undef.superahorrosanchezpucci.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Producto(
    val id: String,
    val codigo: String = "",
    val nombre: String,
    val descripcion: String = "",
    val precio: Int = 0,
    val marca: String = "",
    val precioEstimado: Int = 0,
    val precioReal: Int? = null,
    val cantidad: Int = 1,
    val comprado: Boolean = false,
    val categoria: Categoria = Categoria.ESENCIAL
) : Parcelable

@Parcelize
enum class Categoria : Parcelable {
    ESENCIAL,
    PRINCIPAL,
    SECUNDARIO
}
