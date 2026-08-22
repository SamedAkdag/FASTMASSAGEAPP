package com.aistudio.pingring.pgrng.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.pingring.pgrng.data.model.PairedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM paired_contacts ORDER BY pairedAt DESC")
    fun getAllContactsFlow(): Flow<List<PairedContactEntity>>

    @Query("SELECT * FROM paired_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): PairedContactEntity?

    @Query("SELECT * FROM paired_contacts WHERE pairingCode = :code LIMIT 1")
    suspend fun getContactByPairingCode(code: String): PairedContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: PairedContactEntity)

    @Delete
    suspend fun deleteContact(contact: PairedContactEntity)

    @Query("DELETE FROM paired_contacts WHERE id = :id")
    suspend fun deleteContactById(id: String)
}
