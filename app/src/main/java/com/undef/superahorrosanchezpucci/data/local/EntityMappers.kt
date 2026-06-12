package com.undef.superahorrosanchezpucci.data.local

import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.MetodoPago
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Producto
import com.undef.superahorrosanchezpucci.data.model.RolUsuario
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketProducto
import com.undef.superahorrosanchezpucci.data.model.TipoPresupuesto
import com.undef.superahorrosanchezpucci.data.model.Usuario

import com.undef.superahorrosanchezpucci.data.remote.dto.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

fun Presupuesto.toEntity() = PresupuestoEntity(
    id = id,
    groupId = groupId,
    tipo = tipo.name,
    nombre = nombre,
    montoTotal = montoTotal,
    montoDisponible = montoDisponible,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    activo = activo
)

fun PresupuestoEntity.toModel() = Presupuesto(
    id = id,
    groupId = groupId,
    tipo = if (tipo == "FAMILIAR") TipoPresupuesto.FAMILIAR else TipoPresupuesto.INDIVIDUAL,
    nombre = nombre,
    montoTotal = montoTotal,
    montoDisponible = montoDisponible,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    activo = activo
)

fun BudgetResponse.toModel(isIndividual: Boolean, isActive: Boolean) = Presupuesto(
    id = id,
    groupId = groupId,
    tipo = if (isIndividual) TipoPresupuesto.INDIVIDUAL else TipoPresupuesto.FAMILIAR,
    nombre = name,
    montoTotal = totalAmount.toInt(),
    montoDisponible = 0,
    fechaInicio = parseDate(startDate),
    fechaFin = endDate?.let { parseDate(it) },
    activo = isActive
)

fun ListaCompra.toEntity() = ListaCompraEntity(
    id = id,
    groupId = groupId,
    nombre = nombre,
    presupuestoId = presupuestoId,
    esFamiliar = esFamiliar,
    fechaCreacion = fechaCreacion,
    hora = hora,
    supermercado = supermercado,
    total = total
)

fun ListaCompraEntity.toModel(productos: List<Producto>) = ListaCompra(
    id = id,
    groupId = groupId,
    nombre = nombre,
    presupuestoId = presupuestoId,
    esFamiliar = esFamiliar,
    fechaCreacion = fechaCreacion,
    hora = hora,
    supermercado = supermercado,
    total = total,
    productos = productos.toMutableList()
)

fun ShoppingListResponse.toModel(groupId: String, presupuestoId: String, esFamiliar: Boolean) = ListaCompra(
    id = id,
    groupId = groupId,
    nombre = name,
    presupuestoId = presupuestoId,
    esFamiliar = esFamiliar,
    fechaCreacion = parseDate(createdAt),
    total = products.sumOf { p ->
        val price = p.finalPrice?.toString()?.toDoubleOrNull()
            ?: p.productPrice?.toString()?.toDoubleOrNull()
            ?: p.unitPrice?.toString()?.toDoubleOrNull()
            ?: p.price?.toString()?.toDoubleOrNull()
            ?: p.subtotal?.toString()?.toDoubleOrNull()
            ?: p.totalPrice?.toString()?.toDoubleOrNull()
            ?: 0.0
        val qty = p.finalQuantity?.toString()?.toDoubleOrNull()
            ?: p.quantity?.toString()?.toDoubleOrNull()
            ?: 1.0
        price * qty
    }.toInt(),
    productos = products.map { it.toModel() }.toMutableList()
)

fun ShoppingListProductResponse.toModel(): Producto {
    val priceValue = finalPrice?.toString()?.toDoubleOrNull()
        ?: productPrice?.toString()?.toDoubleOrNull()
        ?: unitPrice?.toString()?.toDoubleOrNull()
        ?: price?.toString()?.toDoubleOrNull()
        ?: subtotal?.toString()?.toDoubleOrNull()
        ?: totalPrice?.toString()?.toDoubleOrNull()
        ?: 0.0
    val qtyValue = finalQuantity?.toString()?.toDoubleOrNull()
        ?: quantity?.toString()?.toDoubleOrNull()
        ?: 1.0
    return Producto(
        id = id,
        nombre = productName,
        codigo = productId,
        precio = priceValue.toInt(),
        precioEstimado = priceValue.toInt(),
        cantidad = qtyValue.toInt(),
        comprado = checked,
        categoria = Categoria.ESENCIAL
    )
}

fun Producto.toEntity(listaId: String) = ProductoEntity(
    id = id,
    listaId = listaId,
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion,
    precio = precio,
    marca = marca,
    precioEstimado = precioEstimado,
    precioReal = precioReal,
    cantidad = cantidad,
    comprado = comprado,
    categoria = categoria.name
)

fun ProductoEntity.toModel() = Producto(
    id = id,
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion,
    precio = precio,
    marca = marca,
    precioEstimado = precioEstimado,
    precioReal = precioReal,
    cantidad = cantidad,
    comprado = comprado,
    categoria = try { enumValueOf<Categoria>(categoria) } catch (_: Exception) { Categoria.ESENCIAL }
)

fun Ticket.toEntity() = TicketEntity(
    id = id,
    groupId = groupId,
    supermercado = supermercado,
    direccion = direccion,
    fechaHora = fechaHora,
    total = total,
    metodoPago = metodoPago.name,
    imagenPath = imagenPath,
    presupuestoId = presupuestoId,
    synced = !id.startsWith("temp-") && id.isNotBlank()
)

fun TicketEntity.toModel(productos: List<TicketProducto>) = Ticket(
    id = id,
    groupId = groupId,
    supermercado = supermercado,
    direccion = direccion,
    fechaHora = fechaHora,
    total = total,
    metodoPago = try { MetodoPago.valueOf(metodoPago) } catch (_: Exception) { MetodoPago.EFECTIVO },
    imagenPath = imagenPath,
    presupuestoId = presupuestoId,
    productos = productos
)

fun PurchaseResponse.toModel(groupId: String, budgetId: String) = Ticket(
    id = id,
    groupId = groupId,
    supermercado = storeName ?: notes ?: "Supermercado",
    direccion = "",
    fechaHora = parseDate(purchaseDate),
    total = (total.toDoubleOrNull()?.toInt() ?: 0),
    metodoPago = MetodoPago.EFECTIVO,
    imagenPath = "",
    presupuestoId = budgetId,
    productos = items.map { it.toModel() }
)

fun PurchaseProductResponse.toModel() = TicketProducto(
    nombre = productName,
    precio = (unitPrice.toDoubleOrNull()?.toInt() ?: 0),
    cantidad = quantity
)

fun TicketProducto.toEntity(ticketId: String, posicion: Int) = TicketProductoEntity(
    ticketId = ticketId,
    posicion = posicion,
    nombre = nombre,
    precio = precio,
    cantidad = cantidad
)

fun TicketProductoEntity.toModel() = TicketProducto(
    nombre = nombre,
    precio = precio,
    cantidad = cantidad
)

fun TicketProductDetection.toModel() = TicketProducto(
    nombre = name,
    precio = (totalPrice ?: unitPrice ?: 0.0).toInt(),
    cantidad = (quantity ?: 1.0).toInt()
)

fun Usuario.toEntity() = UsuarioEntity(
    id = id,
    nombre = nombre,
    email = email,
    rol = rol.name,
    activo = activo
)

fun UsuarioEntity.toModel() = Usuario(
    id = id,
    nombre = nombre,
    email = email,
    rol = try { enumValueOf<RolUsuario>(rol) } catch (_: Exception) { RolUsuario.MIEMBRO },
    activo = activo
)

private fun parseDate(date: String): Long = try {
    LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (_: Exception) { System.currentTimeMillis() }


