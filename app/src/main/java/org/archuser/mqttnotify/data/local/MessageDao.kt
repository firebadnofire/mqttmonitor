package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE broker_id = :brokerId ORDER BY received_at DESC LIMIT 500")
    fun observeForBroker(brokerId: Long): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE broker_id = :brokerId AND topic_filter = :topic AND received_at < :cutoff")
    suspend fun deleteOlderThan(brokerId: Long, topic: String, cutoff: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun countForTopic(brokerId: Long, topic: String): Int

    @Query(
        "DELETE FROM messages WHERE id IN (" +
            "SELECT id FROM messages WHERE broker_id = :brokerId AND topic_filter = :topic " +
            "ORDER BY received_at DESC LIMIT -1 OFFSET :keep)"
    )
    suspend fun deleteOverflowForTopic(brokerId: Long, topic: String, keep: Int)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)
}
