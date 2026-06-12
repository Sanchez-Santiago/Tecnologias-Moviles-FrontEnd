package com.undef.superahorrosanchezpucci.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PresupuestoEntity::class,
        ListaCompraEntity::class,
        ProductoEntity::class,
        TicketEntity::class,
        TicketProductoEntity::class,
        UsuarioEntity::class,
        CatalogoProductoEntity::class,
        TiendaEntity::class,
        GrupoEntity::class,
        InvitacionEntity::class,
        NotificationCacheEntity::class,
        OfferCacheEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "super_ahorro.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
