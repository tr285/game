package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_profile WHERE id = 1 LIMIT 1")
    fun getGameProfileFlow(): Flow<GameProfile?>

    @Query("SELECT * FROM game_profile WHERE id = 1 LIMIT 1")
    suspend fun getGameProfileDirect(): GameProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateGameProfile(profile: GameProfile)

    @Query("SELECT * FROM unlocked_skins")
    fun getUnlockedSkinsFlow(): Flow<List<UnlockedSkin>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockSkin(skin: UnlockedSkin)

    @Query("SELECT * FROM unlocked_badges")
    fun getUnlockedBadgesFlow(): Flow<List<UnlockedBadge>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockBadge(badge: UnlockedBadge)

    @Query("SELECT * FROM talent_upgrades")
    fun getTalentUpgradesFlow(): Flow<List<TalentUpgrade>>

    @Query("SELECT * FROM talent_upgrades")
    suspend fun getTalentUpgradesDirect(): List<TalentUpgrade>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTalentUpgrade(upgrade: TalentUpgrade)
}
