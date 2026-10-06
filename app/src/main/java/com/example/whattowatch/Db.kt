package com.example.whattowatch

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

const val STATUS_LATER = "later"
const val STATUS_WATCHED = "watched"
const val STATUS_HIDDEN = "hidden"

@Entity(tableName = "marks", primaryKeys = ["id", "kind"])
data class Mark(
    val id: Int,
    val kind: String,
    val status: String,
    val title: String,
    val posterPath: String?,
    val addedAt: Long
)

@Dao
interface MarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(mark: Mark)

    @Query("SELECT id FROM marks WHERE kind = :kind")
    suspend fun ids(kind: String): List<Int>

    @Query("SELECT * FROM marks WHERE status = 'later' ORDER BY addedAt DESC")
    fun later(): Flow<List<Mark>>

    @Query("UPDATE marks SET status = :status WHERE id = :id AND kind = :kind")
    suspend fun setStatus(id: Int, kind: String, status: String)

    @Query("DELETE FROM marks WHERE id = :id AND kind = :kind")
    suspend fun delete(id: Int, kind: String)
}

@Database(entities = [Mark::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun marks(): MarkDao
}
