package app.puffling.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One practice done. [practice] is a built-in id or «u<id>»; «bolt» is the breath-hold test, «bell» a bell answered.
 * [holds] are the hold times of a Wim Hof session (or the test), seconds, comma-separated.
 * [before] and [after] are how tense it felt, 1 calm .. 5 storm; 0 when not asked.
 * [bpm] breaths a minute the phone on the belly counted, 0 when it wasn't used. [notes] are gratitude lines, one a line.
 */
@Entity(indices = [Index("start")])
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long,
    val secs: Int,
    val practice: String,
    val kind: String,
    val el: String,
    val rounds: Int = 0,
    val holds: String = "",
    val bpm: Double = 0.0,
    val before: Int = 0,
    val after: Int = 0,
    val notes: String = "",
    val said: Int = 0,              // affirmations said out loud
    val noted: String = "",         // noting counts, «think:4,hear:2,feel:1»
    val food: Double = 0.0,
    val shells: Int = 0,
    val leader: String = "",
    val depth: Int = 0,             // metres the leader dived (Wim Hof)
    val course: String = "",        // «course:day» when done as a course day
    val name: String = "",          // the practice's name then (own practices can be renamed or removed)
) {
    fun holdList(): List<Int> = holds.split(',').mapNotNull { it.trim().toIntOrNull() }
}

/** A practice the user made. Steps are lines «seconds|text». */
@Entity
data class OwnPractice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,               // Kind.code
    val el: String,
    val about: String = "",
    val minutes: Int = 5,
    val pattern: String = "i4,o6",
    val ramp: Double = 0.0,
    val steps: String = "",
    val rounds: Int = 3,
    val breaths: Int = 30,
    val bell: Int = 0,
    val hold: Int = 60,             // Wim Hof: the first round's hold target, seconds
    val deck: String = "mix",
    val archived: Boolean = false,
    val created: Long = 0,
)

/** An affirmation the user wrote. */
@Entity
data class OwnAff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val created: Long = 0,
)

/** An affirmation said out loud, sent to sea; it washes back on [back] and is read once. */
@Entity
data class Bottle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val sent: Long,                 // epoch day
    val back: Long,                 // epoch day it washes ashore
    val opened: Boolean = false,
)

/**
 * A diary entry: the feelings named (ids «joy:pride», comma-separated), how strong (1..5), what it was about
 * (tag ids), and free text. [food] and [shells] are what it gave.
 */
@Entity(indices = [Index("at")])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val emotions: String = "",
    val strength: Int = 3,
    val tags: String = "",
    val text: String = "",
    val food: Double = 0.0,
    val shells: Int = 0,
)

/** A mindfulness bell that rang; [answered] when the three breaths were taken, 0 if not. */
@Entity
data class Bell(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val answered: Long = 0,
)

/**
 * A pet. Until [hatched] it is an egg and [growth] counts towards hatching.
 * [belly] is calm eaten and not yet digested: it turns into growth at night, while the pet sleeps.
 * [els] counts food by family, «air:120,still:40». [slot]: 0 leader, 1 and 2 companions, -1 resting.
 */
@Entity
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val nature: String,
    val found: Long,
    val hatched: Long = 0,
    val growth: Double = 0.0,
    val belly: Double = 0.0,
    val form: String = "",
    val els: String = "",
    val slot: Int = -1,
    val sessions: Int = 0,
    val shiny: Boolean = false,
)

@Dao
interface Dao0 {
    @Query("SELECT * FROM Session ORDER BY start DESC") fun sessions(): Flow<List<Session>>
    @Query("SELECT * FROM Session ORDER BY start DESC") suspend fun sessionList(): List<Session>
    @Query("SELECT * FROM Session WHERE id = :id") suspend fun session(id: Long): Session?
    @Insert suspend fun insert(s: Session): Long
    @Update suspend fun update(s: Session)
    @Query("DELETE FROM Session WHERE id = :id") suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM OwnPractice ORDER BY created") fun own(): Flow<List<OwnPractice>>
    @Query("SELECT * FROM OwnPractice ORDER BY created") suspend fun ownList(): List<OwnPractice>
    @Insert suspend fun insert(p: OwnPractice): Long
    @Update suspend fun update(p: OwnPractice)

    @Query("SELECT * FROM OwnAff ORDER BY created DESC") fun affs(): Flow<List<OwnAff>>
    @Query("SELECT * FROM OwnAff ORDER BY created DESC") suspend fun affList(): List<OwnAff>
    @Insert suspend fun insert(a: OwnAff): Long
    @Update suspend fun update(a: OwnAff)
    @Query("DELETE FROM OwnAff WHERE id = :id") suspend fun deleteAff(id: Long)

    @Query("SELECT * FROM Bottle ORDER BY back DESC") fun bottles(): Flow<List<Bottle>>
    @Query("SELECT * FROM Bottle ORDER BY back DESC") suspend fun bottleList(): List<Bottle>
    @Insert suspend fun insert(b: Bottle): Long
    @Insert suspend fun insertBottles(b: List<Bottle>)
    @Update suspend fun update(b: Bottle)

    @Query("SELECT * FROM Bell ORDER BY at DESC") fun bells(): Flow<List<Bell>>
    @Query("SELECT * FROM Bell ORDER BY at DESC") suspend fun bellList(): List<Bell>
    @Insert suspend fun insert(b: Bell): Long
    @Update suspend fun update(b: Bell)

    @Query("SELECT * FROM Pet ORDER BY found, id") fun pets(): Flow<List<Pet>>
    @Query("SELECT * FROM Pet ORDER BY found, id") suspend fun petList(): List<Pet>
    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)
    @Update suspend fun updatePets(p: List<Pet>)

    @Query("SELECT * FROM Entry ORDER BY at DESC") fun entries(): Flow<List<Entry>>
    @Query("SELECT * FROM Entry ORDER BY at DESC") suspend fun entryList(): List<Entry>
    @Insert suspend fun insert(e: Entry): Long
    @Update suspend fun update(e: Entry)
    @Query("DELETE FROM Entry WHERE id = :id") suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM Session") suspend fun clearSessions()
    @Query("DELETE FROM Pet") suspend fun clearPets()
    @Query("DELETE FROM Bottle") suspend fun clearBottles()
    @Query("DELETE FROM Bell") suspend fun clearBells()
}

@Database(entities = [Session::class, OwnPractice::class, OwnAff::class, Bottle::class, Bell::class, Pet::class, Entry::class], version = 2, exportSchema = true)
abstract class Db : RoomDatabase() {
    abstract fun dao(): Dao0

    companion object {
        fun create(ctx: Context) = Room.databaseBuilder(ctx, Db::class.java, "puffling.db").fallbackToDestructiveMigration(true).build()
    }
}
