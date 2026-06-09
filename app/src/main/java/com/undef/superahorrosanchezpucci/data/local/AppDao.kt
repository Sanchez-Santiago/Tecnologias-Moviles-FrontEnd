package com.undef.superahorrosanchezpucci.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AppDao {
    @Query("SELECT * FROM presupuestos")
    suspend fun getPresupuestos(): List<PresupuestoEntity>

    @Query("SELECT * FROM listas")
    suspend fun getListas(): List<ListaCompraEntity>

    @Query("SELECT * FROM productos")
    suspend fun getProductos(): List<ProductoEntity>

    @Query("SELECT * FROM tickets")
    suspend fun getTickets(): List<TicketEntity>

    @Query("SELECT * FROM ticket_productos ORDER BY posicion")
    suspend fun getTicketProductos(): List<TicketProductoEntity>

    @Query("SELECT * FROM usuarios")
    suspend fun getUsuarios(): List<UsuarioEntity>

    @Query("SELECT * FROM app_config WHERE id = 'config' LIMIT 1")
    suspend fun getConfig(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresupuestos(items: List<PresupuestoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListas(items: List<ListaCompraEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductos(items: List<ProductoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(items: List<TicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketProductos(items: List<TicketProductoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuarios(items: List<UsuarioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfigEntity)

    @Query("DELETE FROM presupuestos")
    suspend fun clearPresupuestos()

    @Query("DELETE FROM listas")
    suspend fun clearListas()

    @Query("DELETE FROM productos")
    suspend fun clearProductos()

    @Query("DELETE FROM tickets")
    suspend fun clearTickets()

    @Query("DELETE FROM ticket_productos")
    suspend fun clearTicketProductos()

    @Query("DELETE FROM usuarios")
    suspend fun clearUsuarios()

    // Catalogo productos
    @Query("SELECT * FROM catalogo_productos")
    suspend fun getCatalogoProductos(): List<CatalogoProductoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogoProductos(items: List<CatalogoProductoEntity>)

    @Query("DELETE FROM catalogo_productos")
    suspend fun clearCatalogoProductos()

    // Tiendas
    @Query("SELECT * FROM tiendas")
    suspend fun getTiendas(): List<TiendaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiendas(items: List<TiendaEntity>)

    @Query("DELETE FROM tiendas")
    suspend fun clearTiendas()

    // Ticket queries for sync
    @Query("SELECT * FROM tickets WHERE synced = 0")
    suspend fun getUnsyncedTickets(): List<TicketEntity>

    @Query("UPDATE tickets SET synced = 1 WHERE id = :id")
    suspend fun markTicketSynced(id: String)

    @Query("SELECT * FROM ticket_productos WHERE ticketId = :ticketId")
    suspend fun getTicketProductosByTicketId(ticketId: String): List<TicketProductoEntity>

    @Transaction
    suspend fun replaceAll(
        presupuestos: List<PresupuestoEntity>,
        listas: List<ListaCompraEntity>,
        productos: List<ProductoEntity>,
        tickets: List<TicketEntity>,
        ticketProductos: List<TicketProductoEntity>,
        usuarios: List<UsuarioEntity>,
        config: AppConfigEntity
    ) {
        clearTicketProductos()
        clearProductos()
        clearTickets()
        clearListas()
        clearPresupuestos()
        clearUsuarios()

        insertPresupuestos(presupuestos)
        insertListas(listas)
        insertProductos(productos)
        insertTickets(tickets)
        insertTicketProductos(ticketProductos)
        insertUsuarios(usuarios)
        insertConfig(config)
    }
}
