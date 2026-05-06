package com.example.misuper.data.model

import com.example.misuper.ui.theme.ThemeMode

data class AppData(
    val presupuestos: List<Presupuesto>,
    val listas: List<ListaCompra>,
    val tickets: List<Ticket>,
    val usuarios: List<Usuario>,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)