package com.example.data.model

import androidx.compose.ui.graphics.Color

data class SkinConfig(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val mainColor: Color,
    val accentColor: Color,
    val effectDesc: String
)

data class BadgeConfig(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val requirement: String
)

data class TalentConfig(
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Int,
    val costMultiplier: Float,
    val maxLevel: Int,
    val emoji: String
)

object GameConfig {
    val SKINS = listOf(
        SkinConfig(
            id = "SIGMA_GLOW",
            name = "Sigma Glow",
            description = "The default cyan neon saber of ultimate concentration.",
            cost = 0,
            mainColor = Color(0xFF00FFCC),
            accentColor = Color(0xFF00E6FF),
            effectDesc = "Default. Standard Slash Area."
        ),
        SkinConfig(
            id = "CAP_BUSTER",
            name = "Cap Buster",
            description = "A hazardous yellow industrial beam that crushes lies.",
            cost = 150,
            mainColor = Color(0xFFFFD700),
            accentColor = Color(0xFFFF8C00),
            effectDesc = "+15% Extra Coin Drop Rate"
        ),
        SkinConfig(
            id = "BASED_KATANA",
            name = "Based Katana",
            description = "Deep magenta frequency blade with immense ripple force.",
            cost = 350,
            mainColor = Color(0xFFFF007F),
            accentColor = Color(0xFF8B008B),
            effectDesc = "+20% Critical Hit Damage"
        ),
        SkinConfig(
            id = "RIZZ_DAGGER",
            name = "Rizz Dagger",
            description = "Super fast ruby dagger of pure magnetism.",
            cost = 600,
            mainColor = Color(0xFFFF3333),
            accentColor = Color(0xFFFF5E5E),
            effectDesc = "+15% Cyber-Slash Cooldown Speed"
        ),
        SkinConfig(
            id = "GIGACHAD_CLEAVER",
            name = "GigaChad Cleaver",
            description = "An absolute emerald slab of solid alpha code.",
            cost = 1000,
            mainColor = Color(0xFF00FF33),
            accentColor = Color(0xFF008000),
            effectDesc = "+30% Larger Slash Sweep Area"
        ),
        SkinConfig(
            id = "RAINBOW_SKIBIDI",
            name = "Rainbow Skibidi",
            description = "Unstable, multi-frequency rainbow emitter. Total brainrot.",
            cost = 2000,
            mainColor = Color(0xFF9900FF),
            accentColor = Color(0xFFFF00FF),
            effectDesc = "Rainbow Particles & 2x XP Crystals"
        )
    )

    val BADGES = listOf(
        BadgeConfig(
            id = "NOOB_VIBE",
            name = "Lvl 1 Noob",
            description = "Just downloaded the app, pure untouched potential.",
            emoji = "🐣",
            requirement = "Default unlocked badge."
        ),
        BadgeConfig(
            id = "SIGMA_GRIND",
            name = "Sigma Grind",
            description = "Killed 50 viral glitch entities in a single run.",
            emoji = "🗿",
            requirement = "Defeat 50 glitches in one session."
        ),
        BadgeConfig(
            id = "BASED_EARNER",
            name = "Based Chief",
            description = "Amassed 1,000 total Vibe Coins in career.",
            emoji = "💰",
            requirement = "Reach 1,000 career coins."
        ),
        BadgeConfig(
            id = "RIZZ_MASTER",
            name = "Rizz Lord",
            description = "Upgraded any vibe talent to max level.",
            emoji = "🔥",
            requirement = "Upgrade a talent to Level 5."
        ),
        BadgeConfig(
            id = "SURVIVOR_GOAT",
            name = "Glitch Dodger",
            description = "Survived for over 100 seconds in a single run.",
            emoji = "🐐",
            requirement = "Survive 100s in one session."
        ),
        BadgeConfig(
            id = "CHAD_ENERGY",
            name = "Main Character",
            description = "Reached Character Level 10.",
            emoji = "👑",
            requirement = "Reach Level 10 in-game."
        )
    )

    val TALENTS = listOf(
        TalentConfig(
            id = "DAMAGE",
            name = "Vibe Slap",
            description = "Increases slash damage to tear through larger glitches.",
            baseCost = 50,
            costMultiplier = 1.6f,
            maxLevel = 5,
            emoji = "💥"
        ),
        TalentConfig(
            id = "COOLDOWN",
            name = "Hyper-Scroll",
            description = "Reduces delay between auto-slashes and active taps.",
            baseCost = 75,
            costMultiplier = 1.8f,
            maxLevel = 5,
            emoji = "⚡"
        ),
        TalentConfig(
            id = "CRIT",
            name = "Rizz Strike",
            description = "Increases critical hit chance by 8% per level.",
            baseCost = 100,
            costMultiplier = 1.7f,
            maxLevel = 5,
            emoji = "🎯"
        ),
        TalentConfig(
            id = "SHIELD",
            name = "Aura Shield",
            description = "Blocks any incoming glitch hit. Regenerates in 15 seconds.",
            baseCost = 150,
            costMultiplier = 2.0f,
            maxLevel = 5,
            emoji = "🛡️"
        ),
        TalentConfig(
            id = "MAGNET",
            name = "Coin Puller",
            description = "Increases magnet pickup range for XP and coin drops.",
            baseCost = 60,
            costMultiplier = 1.5f,
            maxLevel = 5,
            emoji = "🧲"
        ),
        TalentConfig(
            id = "XP_BOOST",
            name = "Brain Gain",
            description = "Gain 15% more XP per crystal collected.",
            baseCost = 80,
            costMultiplier = 1.6f,
            maxLevel = 5,
            emoji = "🧠"
        )
    )

    fun getSkin(id: String) = SKINS.firstOrNull { it.id == id } ?: SKINS.first()
    fun getBadge(id: String) = BADGES.firstOrNull { it.id == id } ?: BADGES.first()
    fun getTalent(id: String) = TALENTS.firstOrNull { it.id == id }
}
