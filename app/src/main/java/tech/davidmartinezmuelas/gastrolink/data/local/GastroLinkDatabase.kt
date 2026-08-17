package tech.davidmartinezmuelas.gastrolink.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        ParticipantEntity::class,
        ProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GastroLinkDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: GastroLinkDatabase? = null

        fun getInstance(context: Context): GastroLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastroLinkDatabase::class.java,
                    "gastrolink_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
