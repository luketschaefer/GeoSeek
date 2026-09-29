package com.geoseek.collector.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoseek.core.database.GeoSeekDatabase
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.profile.data.RoomProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomCollectionRepositoryTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                GeoSeekDatabase::class.java,
            ).build()
    private val collection = RoomCollectionRepository(db)
    private val profile = RoomProfileRepository(db)

    @After
    fun tearDown() = db.close()

    @Test
    fun firstAcquisitionAwardsXpOnceAndKeepsFirstTimestamp() =
        runTest {
            val first = collection.acquire(ObjectId("cup"), xpIfFirst = 5, at = Instant.ofEpochMilli(1_000))
            val second = collection.acquire(ObjectId("cup"), xpIfFirst = 5, at = Instant.ofEpochMilli(2_000))

            assertThat(first.isFirstAcquisition).isTrue()
            assertThat(second.isFirstAcquisition).isFalse()
            assertThat(second.card.count).isEqualTo(2)
            assertThat(second.card.firstAcquiredAt).isEqualTo(Instant.ofEpochMilli(1_000))
            assertThat(profile.observeProfile().first().totalXp).isEqualTo(5)
        }
}
