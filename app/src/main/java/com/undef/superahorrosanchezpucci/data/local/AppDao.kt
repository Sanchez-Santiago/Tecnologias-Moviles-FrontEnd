package com.undef.superahorrosanchezpucci.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AppDao {
    @Query("SELECT * FROM presupuestos")
    abstract suspend fun getPresupuestos(): List<PresupuestoEntity>

    @Query("SELECT * FROM listas")
    abstract suspend fun getListas(): List<ListaCompraEntity>

    @Query("SELECT * FROM productos")
    abstract suspend fun getProductos(): List<ProductoEntity>

    @Query("SELECT * FROM tickets")
    abstract suspend fun getTickets(): List<TicketEntity>

    @Query("SELECT * FROM ticket_productos ORDER BY posicion")
    abstract suspend fun getTicketProductos(): List<TicketProductoEntity>

    @Query("SELECT * FROM usuarios")
    abstract suspend fun getUsuarios(): List<UsuarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPresupuestos(items: List<PresupuestoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertListas(items: List<ListaCompraEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertProductos(items: List<ProductoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTickets(items: List<TicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTicket(item: TicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTicketProductos(items: List<TicketProductoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUsuarios(items: List<UsuarioEntity>)

    @Transaction
    open suspend fun saveFullTicket(ticket: TicketEntity, productos: List<TicketProductoEntity>) {
        deleteTicketProductosByTicketId(ticket.id)
        insertTicket(ticket)
        insertTicketProductos(productos)
    }

    @Query("DELETE FROM tickets WHERE id = :id")
    abstract suspend fun deleteTicketById(id: String)

    @Query("DELETE FROM ticket_productos WHERE ticketId = :ticketId")
    abstract suspend fun deleteTicketProductosByTicketId(ticketId: String)

    @Transaction
    open suspend fun deleteFullTicket(id: String) {
        deleteTicketProductosByTicketId(id)
        deleteTicketById(id)
    }

    @Query("DELETE FROM presupuestos")
    abstract suspend fun clearPresupuestos()

    @Query("DELETE FROM listas")
    abstract suspend fun clearListas()

    @Query("DELETE FROM productos")
    abstract suspend fun clearProductos()

    @Query("DELETE FROM tickets")
    abstract suspend fun clearTickets()

    @Query("DELETE FROM ticket_productos")
    abstract suspend fun clearTicketProductos()

    @Query("SELECT * FROM catalogo_productos WHERE name LIKE '%' || :query || '%'")
    abstract suspend fun searchCatalogo(query: String): List<CatalogoProductoEntity>

    @Query("DELETE FROM usuarios")
    abstract suspend fun clearUsuarios()

    // Catalogo productos
    @Query("SELECT * FROM catalogo_productos")
    abstract suspend fun getCatalogoProductos(): List<CatalogoProductoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCatalogoProductos(items: List<CatalogoProductoEntity>)

    @Query("DELETE FROM catalogo_productos")
    abstract suspend fun clearCatalogoProductos()

    // Tiendas
    @Query("SELECT * FROM tiendas")
    abstract suspend fun getTiendas(): List<TiendaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTiendas(items: List<TiendaEntity>)

    @Query("DELETE FROM tiendas")
    abstract suspend fun clearTiendas()

    // Ticket queries for sync
    @Query("SELECT * FROM tickets WHERE synced = 0")
    abstract suspend fun getUnsyncedTickets(): List<TicketEntity>

    @Query("UPDATE tickets SET synced = 1 WHERE id = :id")
    abstract suspend fun markTicketSynced(id: String)

    @Query("SELECT * FROM ticket_productos WHERE ticketId = :ticketId")
    abstract suspend fun getTicketProductosByTicketId(ticketId: String): List<TicketProductoEntity>

    // Grupos
    @Query("SELECT * FROM grupos")
    abstract suspend fun getGrupos(): List<GrupoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGrupos(items: List<GrupoEntity>)

    @Query("DELETE FROM grupos")
    abstract suspend fun clearGrupos()

    // Invitaciones
    @Query("SELECT * FROM invitaciones")
    abstract suspend fun getInvitaciones(): List<InvitacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertInvitaciones(items: List<InvitacionEntity>)

    @Query("DELETE FROM invitaciones")
    abstract suspend fun clearInvitaciones()

    // Notifications cache
    @Query("SELECT * FROM notifications_cache ORDER BY createdAt DESC")
    abstract suspend fun getCachedNotifications(): List<NotificationCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCachedNotifications(items: List<NotificationCacheEntity>)

    @Query("DELETE FROM notifications_cache")
    abstract suspend fun clearCachedNotifications()

    // Offers cache
    @Query("SELECT * FROM offers_cache")
    abstract suspend fun getCachedOffers(): List<OfferCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCachedOffers(items: List<OfferCacheEntity>)

    @Query("DELETE FROM offers_cache")
    abstract suspend fun clearCachedOffers()

    // Lista de compra productos
    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    abstract suspend fun getProductosByListaId(listaId: String): List<ProductoEntity>

    @Query("DELETE FROM productos WHERE listaId = :listaId")
    abstract suspend fun deleteProductosByListaId(listaId: String)

    @Query("DELETE FROM tickets WHERE groupId = :groupId")
    abstract suspend fun deleteTicketsByGroupId(groupId: String)

    @Query("DELETE FROM ticket_productos WHERE ticketId IN (SELECT id FROM tickets WHERE groupId = :groupId)")
    abstract suspend fun deleteTicketProductosByGroupId(groupId: String)

    @Query("UPDATE tickets SET groupId = :newGroupId WHERE groupId = ''")
    abstract suspend fun migrateTicketsToGroup(newGroupId: String)

    @Query("UPDATE presupuestos SET groupId = :newGroupId WHERE groupId = ''")
    abstract suspend fun migratePresupuestosToGroup(newGroupId: String)

    @Query("UPDATE listas SET groupId = :newGroupId WHERE groupId = ''")
    abstract suspend fun migrateListasToGroup(newGroupId: String)

    @Transaction
    open suspend fun refreshTickets(groupId: String, tickets: List<TicketEntity>, productos: List<TicketProductoEntity>) {
        deleteTicketProductosByGroupId(groupId)
        deleteTicketsByGroupId(groupId)
        insertTickets(tickets)
        insertTicketProductos(productos)
    }

    @Transaction
    open suspend fun refreshListas(listas: List<ListaCompraEntity>, productos: List<ProductoEntity>) {
        val listaIds = listas.map { it.id }
        listaIds.forEach { deleteProductosByListaId(it) }
        insertListas(listas)
        insertProductos(productos)
    }
}
