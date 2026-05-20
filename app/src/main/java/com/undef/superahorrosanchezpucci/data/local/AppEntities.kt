package com.undef.superahorrosanchezpucci.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presupuestos")
data class PresupuestoEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val nombre: String,
    val montoTotal: Int,
    val montoDisponible: Int,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val activo: Boolean
)

@Entity(tableName = "listas")
data class ListaCompraEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val presupuestoId: String,
    val esFamiliar: Boolean,
    val fechaCreacion: Long,
    val hora: String,
    val supermercado: String,
    val total: Int
)

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey val id: String,
    val listaId: String,
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val precio: Int,
    val marca: String,
    val precioEstimado: Int,
    val precioReal: Int?,
    val cantidad: Int,
    val comprado: Boolean,
    val categoria: String
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val supermercado: String,
    val direccion: String,
    val fechaHora: Long,
    val total: Int,
    val metodoPago: String,
    val imagenPath: String,
    val presupuestoId: String
)

@Entity(tableName = "ticket_productos", primaryKeys = ["ticketId", "posicion"])
data class TicketProductoEntity(
    val ticketId: String,
    val posicion: Int,
    val nombre: String,
    val precio: Int,
    val cantidad: Int
)

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: String = "config",
    val themeMode: String
)
