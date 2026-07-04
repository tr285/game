package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.GameConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(private val gameDao: GameDao) {

    val gameProfile: Flow<GameProfile?> = gameDao.getGameProfileFlow()
    val unlockedSkins: Flow<List<UnlockedSkin>> = gameDao.getUnlockedSkinsFlow()
    val unlockedBadges: Flow<List<UnlockedBadge>> = gameDao.getUnlockedBadgesFlow()
    val talentUpgrades: Flow<List<TalentUpgrade>> = gameDao.getTalentUpgradesFlow()

    suspend fun ensureProfileExists() {
        val current = gameDao.getGameProfileDirect()
        if (current == null) {
            gameDao.updateGameProfile(GameProfile())
            gameDao.unlockSkin(UnlockedSkin("SIGMA_GLOW"))
            gameDao.unlockBadge(UnlockedBadge("NOOB_VIBE"))
        }
    }

    suspend fun saveGameSession(
        coinsEarned: Int,
        xpEarned: Float,
        killsInSession: Int,
        timeSurvivedSeconds: Int
    ) {
        ensureProfileExists()
        val current = gameDao.getGameProfileDirect() ?: GameProfile()

        // Calculate XP and level up
        var newXp = current.xp + xpEarned
        var newLevel = current.level
        var xpRequired = getXpForLevel(newLevel)

        while (newXp >= xpRequired) {
            newXp -= xpRequired
            newLevel++
            xpRequired = getXpForLevel(newLevel)
        }

        val totalCoins = current.coins + coinsEarned
        val totalKills = current.totalKills + killsInSession
        val totalTime = current.totalPlayTimeSeconds + timeSurvivedSeconds
        val newHighScore = if (killsInSession > current.highScore) killsInSession else current.highScore

        val updatedProfile = current.copy(
            level = newLevel,
            xp = newXp,
            coins = totalCoins,
            highScore = newHighScore,
            totalKills = totalKills,
            totalPlayTimeSeconds = totalTime
        )

        gameDao.updateGameProfile(updatedProfile)

        // Automatic Badge Unlocks
        if (killsInSession >= 50) {
            gameDao.unlockBadge(UnlockedBadge("SIGMA_GRIND"))
        }
        if (totalCoins >= 1000) {
            gameDao.unlockBadge(UnlockedBadge("BASED_EARNER"))
        }
        if (timeSurvivedSeconds >= 100) {
            gameDao.unlockBadge(UnlockedBadge("SURVIVOR_GOAT"))
        }
        if (newLevel >= 10) {
            gameDao.unlockBadge(UnlockedBadge("CHAD_ENERGY"))
        }
    }

    suspend fun purchaseSkin(skinId: String, cost: Int): Boolean {
        val current = gameDao.getGameProfileDirect() ?: return false
        if (current.coins >= cost) {
            val updated = current.copy(coins = current.coins - cost)
            gameDao.updateGameProfile(updated)
            gameDao.unlockSkin(UnlockedSkin(skinId))
            return true
        }
        return false
    }

    suspend fun equipSkin(skinId: String) {
        val current = gameDao.getGameProfileDirect() ?: return
        gameDao.updateGameProfile(current.copy(selectedSkin = skinId))
    }

    suspend fun equipBadge(badgeId: String) {
        val current = gameDao.getGameProfileDirect() ?: return
        gameDao.updateGameProfile(current.copy(selectedBadge = badgeId))
    }

    suspend fun purchaseTalent(talentId: String, cost: Int): Boolean {
        val current = gameDao.getGameProfileDirect() ?: return false
        if (current.coins >= cost) {
            val currentUpgrades = gameDao.getTalentUpgradesDirect()
            val existingUpgrade = currentUpgrades.firstOrNull { it.talentId == talentId }
            val currentLevel = existingUpgrade?.level ?: 0
            
            val config = GameConfig.getTalent(talentId) ?: return false
            if (currentLevel < config.maxLevel) {
                // Deduct coins
                val updatedProfile = current.copy(coins = current.coins - cost)
                gameDao.updateGameProfile(updatedProfile)
                
                // Save talent level increment
                val nextLevel = currentLevel + 1
                gameDao.saveTalentUpgrade(TalentUpgrade(talentId, nextLevel))

                // Unlock RIZZ_MASTER badge if we just maxed any talent
                if (nextLevel == config.maxLevel) {
                    gameDao.unlockBadge(UnlockedBadge("RIZZ_MASTER"))
                }
                return true
            }
        }
        return false
    }

    fun getXpForLevel(level: Int): Float {
        return 100f + (level - 1) * 50f
    }
}
