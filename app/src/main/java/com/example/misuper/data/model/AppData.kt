package com.example.misuper.data.model

data class AppData(
    val presupuestos: List<Presupuesto>,
    val listas: List<ListaCompra>,
    val tickets: List<Ticket>,
    val usuarios: List<Usuario>
)