package com.example.misuper.data.model

data class ListaCompra(
    val id: String,
    val nombre: String,
    val presupuestoId: String = "",
    val esFamiliar: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val hora: String = "",
    val supermercado: String = "",
    val total: Int = 0,
    val productos: MutableList<Producto> = mutableListOf()
)