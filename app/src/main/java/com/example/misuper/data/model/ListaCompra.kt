package com.example.misuper.data.model

data class ListaCompra(
    val id: String,
    val nombre: String,
    val presupuestoId: String,
    val esFamiliar: Boolean,
    val fechaCreacion: Long,
    val productos: MutableList<Producto>
)