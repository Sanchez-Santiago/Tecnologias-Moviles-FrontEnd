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
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode

fun Presupuesto.toEntity() = PresupuestoEntity(
    id = id,
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
    tipo = enumValueOf<TipoPresupuesto>(tipo),
    nombre = nombre,
    montoTotal = montoTotal,
    montoDisponible = montoDisponible,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    activo = activo
)

fun ListaCompra.toEntity() = ListaCompraEntity(
    id = id,
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
    nombre = nombre,
    presupuestoId = presupuestoId,
    esFamiliar = esFamiliar,
    fechaCreacion = fechaCreacion,
    hora = hora,
    supermercado = supermercado,
    total = total,
    productos = productos.toMutableList()
)

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
    categoria = enumValueOf<Categoria>(categoria)
)

fun Ticket.toEntity() = TicketEntity(
    id = id,
    supermercado = supermercado,
    direccion = direccion,
    fechaHora = fechaHora,
    total = total,
    metodoPago = metodoPago.name,
    imagenPath = imagenPath,
    presupuestoId = presupuestoId
)

fun TicketEntity.toModel(productos: List<TicketProducto>) = Ticket(
    id = id,
    supermercado = supermercado,
    direccion = direccion,
    fechaHora = fechaHora,
    total = total,
    metodoPago = enumValueOf<MetodoPago>(metodoPago),
    imagenPath = imagenPath,
    presupuestoId = presupuestoId,
    productos = productos
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
    rol = enumValueOf<RolUsuario>(rol),
    activo = activo
)

fun ThemeMode.toEntity() = AppConfigEntity(themeMode = name)

fun AppConfigEntity.toThemeMode() = enumValueOf<ThemeMode>(themeMode)
