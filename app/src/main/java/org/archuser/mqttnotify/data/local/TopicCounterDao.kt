package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TopicCounterDao {
    @Query("SELECT * FROM topic_counters WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun getCounter(brokerId: Long, topic: String): TopicCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TopicCounterEntity)

    @Query("SELECT COALESCE(SUM(unread_count), 0) FROM topic_counters WHERE broker_id = :brokerId")
    suspend fun unreadCountForBroker(brokerId: Long): Int

    @Query("UPDATE topic_counters SET unread_count = 0 WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun resetUnreadForTopic(brokerId: Long, topic: String)
}
