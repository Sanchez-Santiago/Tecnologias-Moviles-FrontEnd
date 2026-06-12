package com.undef.superahorrosanchezpucci.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "presupuestos")
data class PresupuestoEntity(
    @PrimaryKey val id: String,
    val groupId: String,
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
    val groupId: String,
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
    val groupId: String,
    val supermercado: String,
    val direccion: String,
    val fechaHora: Long,
    val total: Int,
    val metodoPago: String,
    val imagenPath: String,
    val presupuestoId: String,
    val storeId: String? = null,
    val notes: String? = null,
    val userId: String? = null,
    val synced: Boolean = false
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

@Entity(tableName = "catalogo_productos")
data class CatalogoProductoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val categoryId: String,
    val categoryName: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val barcode: String? = null,
    val priority: String,
    val active: Boolean
)

@Entity(tableName = "tiendas", indices = [Index(value = ["name"], unique = true)])
data class TiendaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val active: Boolean
)

@Entity(tableName = "grupos")
data class GrupoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val categoria: String? = "FAMILIA",
    val createdBy: String,
    val membersJson: String,
    val createdAt: String = ""
)

@Entity(tableName = "invitaciones")
data class InvitacionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val groupName: String,
    val invitedBy: String,
    val invitedByEmail: String,
    val status: String,
    val token: String,
    val expiresAt: String = "",
    val createdAt: String = ""
)

@Entity(tableName = "notifications_cache")
data class NotificationCacheEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val message: String,
    val data: String? = null,
    val read: Boolean = false,
    val createdAt: String = ""
)

@Entity(tableName = "offers_cache")
data class OfferCacheEntity(
    @PrimaryKey val id: String,
    val storeId: String? = null,
    val storeName: String? = null,
    val title: String,
    val description: String? = null,
    val discountType: String,
    val discountValue: Double,
    val startDate: String = "",
    val endDate: String = "",
    val imageUrl: String? = null,
    val active: Boolean = true
)
