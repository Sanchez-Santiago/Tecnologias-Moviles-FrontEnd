package com.example.misuper.data.model

data class Producto(
    val id: String,
    val nombre: String,
    val marca: String,
    val precioEstimado: Int,
    val precioReal: Int?,
    val cantidad: Int,
    val comprado: Boolean,
    val categoria: Categoria
)

enum class Categoria {
    ESENCIAL,
    PRINCIPAL,
    SECUNDARIO
}