package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profile")
data class GameProfile(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Float = 0f,
    val coins: Int = 0,
    val highScore: Int = 0,
    val selectedSkin: String = "SIGMA_GLOW",
    val selectedBadge: String = "NOOB_VIBE",
    val totalKills: Int = 0,
    val totalPlayTimeSeconds: Int = 0
)

@Entity(tableName = "unlocked_skins")
data class UnlockedSkin(
    @PrimaryKey val skinId: String
)

@Entity(tableName = "unlocked_badges")
data class UnlockedBadge(
    @PrimaryKey val badgeId: String
)

@Entity(tableName = "talent_upgrades")
data class TalentUpgrade(
    @PrimaryKey val talentId: String,
    val level: Int = 0
)
