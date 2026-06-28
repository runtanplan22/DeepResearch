package com.deepresearch.app.data.local

import android.content.Context
import androidx.room.*

/**
 * Room database for caching reports and research state.
 */
@Database(entities = [ReportCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportCacheDao(): ReportCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deep_research_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "report_cache")
data class ReportCacheEntity(
    @PrimaryKey val id: String = "last_report",
    val topic: String,
    val report: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ReportCacheDao {
    @Query("SELECT * FROM report_cache WHERE id = :id")
    suspend fun getReport(id: String = "last_report"): ReportCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReport(report: ReportCacheEntity)

    @Query("DELETE FROM report_cache")
    suspend fun clearAll()
}
