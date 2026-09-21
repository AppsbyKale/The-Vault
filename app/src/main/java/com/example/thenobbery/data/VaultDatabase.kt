package com.example.thenobbery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.thenobbery.data.Asset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VaultConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String = Gson().toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
}

@Database(entities = [Asset::class], version = 4, exportSchema = false)
@TypeConverters(VaultConverters::class)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE assets ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE assets ADD COLUMN collection TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new columns as JSON lists
                database.execSQL("ALTER TABLE assets ADD COLUMN pntrPaths TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE assets ADD COLUMN svgPaths TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE assets ADD COLUMN transparentPngPaths TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE assets ADD COLUMN backgroundPngPaths TEXT NOT NULL DEFAULT '[]'")

                // Wrap existing single-path values in JSON arrays
                database.execSQL("UPDATE assets SET pntrPaths = '[\"' || pntrPath || '\"]' WHERE pntrPath IS NOT NULL AND pntrPath != ''")
                database.execSQL("UPDATE assets SET svgPaths = '[\"' || svgPath || '\"]' WHERE svgPath IS NOT NULL AND svgPath != ''")
                database.execSQL("UPDATE assets SET transparentPngPaths = '[\"' || transparentPngPath || '\"]' WHERE transparentPngPath IS NOT NULL AND transparentPngPath != ''")
                database.execSQL("UPDATE assets SET backgroundPngPaths = '[\"' || backgroundPngPath || '\"]' WHERE backgroundPngPath IS NOT NULL AND backgroundPngPath != ''")

                // Drop old single-value columns
                database.execSQL("ALTER TABLE assets DROP COLUMN pntrPath")
                database.execSQL("ALTER TABLE assets DROP COLUMN svgPath")
                database.execSQL("ALTER TABLE assets DROP COLUMN transparentPngPath")
                database.execSQL("ALTER TABLE assets DROP COLUMN backgroundPngPath")
            }
        }

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
