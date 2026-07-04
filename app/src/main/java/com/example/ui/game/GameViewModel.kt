package com.example.ui.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.GameDatabase
import com.example.data.database.GameProfile
import com.example.data.database.TalentUpgrade
import com.example.data.database.UnlockedSkin
import com.example.data.database.UnlockedBadge
import com.example.data.model.GameConfig
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class PlayState {
    IDLE, PLAYING, GAME_OVER
}

enum class GlitchType {
    BOOMER, DOOMSCROLL, MATH_EXAM, BRAIN_ROT, FAKE_NEWS
}

data class Glitch(
    val id: Int,
    val type: GlitchType,
    val name: String,
    val phrase: String,
    var x: Float,
    var y: Float,
    val speed: Float,
    var hp: Float,
    val maxHp: Float,
    val size: Float,
    val color: Color,
    val xpReward: Float,
    val coinReward: Int,
    var isHitFlash: Boolean = false,
    var hitFlashTicks: Int = 0
)

data class Collectible(
    val id: Int,
    val isCoin: Boolean, // true for Coin, false for XP
    var x: Float,
    var y: Float,
    val value: Float
)

data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var alpha: Float,
    val size: Float
)

data class FloatingNumber(
    val id: Int,
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var alpha: Float,
    val isCrit: Boolean = false
)

data class SlashVisual(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val maxDurationTicks: Int = 12,
    var currentTick: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    
    // Core profile flows
    val gameProfile: StateFlow<GameProfile?>
    val unlockedSkins: StateFlow<List<UnlockedSkin>>
    val unlockedBadges: StateFlow<List<UnlockedBadge>>
    val talentUpgrades: StateFlow<List<TalentUpgrade>>

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        
        gameProfile = repository.gameProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
        
        unlockedSkins = repository.unlockedSkins.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        unlockedBadges = repository.unlockedBadges.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        talentUpgrades = repository.talentUpgrades.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.ensureProfileExists()
        }
    }

    // Active Run State
    private val _playState = MutableStateFlow(PlayState.IDLE)
    val playState: StateFlow<PlayState> = _playState.asStateFlow()

    // Game stats in active session
    val runScore = MutableStateFlow(0)
    val runCoins = MutableStateFlow(0)
    val runXp = MutableStateFlow(0f)
    val runXpRequired = MutableStateFlow(100f)
    val runLevel = MutableStateFlow(1)
    val runTimeSeconds = MutableStateFlow(0)
    val runPlayerHp = MutableStateFlow(3)
    val runPlayerMaxHp = MutableStateFlow(3)
    val runShieldActive = MutableStateFlow(false)
    val runShieldCooldownTicks = MutableStateFlow(0)

    // Player position (virtual coordinates 0 to 1000)
    val playerX = MutableStateFlow(500f)
    val playerY = MutableStateFlow(500f)

    // Game collections (thread-safe operations done on main)
    val glitches = MutableStateFlow<List<Glitch>>(emptyList())
    val collectibles = MutableStateFlow<List<Collectible>>(emptyList())
    val particles = MutableStateFlow<List<Particle>>(emptyList())
    val floatingNumbers = MutableStateFlow<List<FloatingNumber>>(emptyList())
    val activeSlashes = MutableStateFlow<List<SlashVisual>>(emptyList())

    // Joystick vectors
    private var joystickDx = 0f
    private var joystickDy = 0f

    private var gameLoopJob: Job? = null
    private val idGenerator = AtomicInteger(1)
    private var spawnTimerTicks = 0
    private var autoSlashCooldownTicks = 0
    private var gameTickCount = 0

    // Cache active modifiers from Talents
    private var talentDmgMultiplier = 1.0f
    private var talentCooldownMultiplier = 1.0f
    private var talentCritChance = 0.05f
    private var talentHasShield = false
    private var talentShieldLevel = 0
    private var talentMagnetRange = 90f
    private var talentXpMultiplier = 1.0f

    private val phrasesBoomer = listOf("Back in my day...", "Lazy Gen-Z!", "No work ethic", "Write in cursive!", "Unc advice", "Put down the phone!")
    private val phrasesDoomscroll = listOf("Just 5 more mins", "Brainrot algorithm", "Infinite swipe", "No attention span", "Tiktok feed", "FOMO trap")
    private val phrasesMathExam = listOf("Find x!", "y = mx + b", "Integral dx", "Fail grade!", "Derivative", "Quadratic formula")
    private val phrasesBrainRot = listOf("Skibidi!", "Rizzler!", "Sigma male!", "Based & Redpilled", "Fanum tax!", "Gyatt!")
    private val phrasesFakeNews = listOf("Clickbait!", "Shocking truth!", "Not clickbait!", "You won't believe", "AI generated!", "Fake leak")

    fun setJoystickVector(dx: Float, dy: Float) {
        joystickDx = dx
        joystickDy = dy
    }

    fun goToLobby() {
        _playState.value = PlayState.IDLE
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun startGame() {
        // Reset active session parameters
        runScore.value = 0
        runCoins.value = 0
        runXp.value = 0f
        runXpRequired.value = 100f
        runLevel.value = 1
        runTimeSeconds.value = 0
        runPlayerHp.value = 3
        runPlayerMaxHp.value = 3
        playerX.value = 500f
        playerY.value = 500f
        
        glitches.value = emptyList()
        collectibles.value = emptyList()
        particles.value = emptyList()
        floatingNumbers.value = emptyList()
        activeSlashes.value = emptyList()

        idGenerator.set(1)
        spawnTimerTicks = 0
        gameTickCount = 0

        // Calculate upgrades from active profiles
        calculateTalentEffects()

        if (talentHasShield) {
            runShieldActive.value = true
            runShieldCooldownTicks.value = 0
        } else {
            runShieldActive.value = false
        }

        // Set state to playing
        _playState.value = PlayState.PLAYING
        GameSound.playPowerUp()

        // Spawn a few initial items
        spawnInitialCollectibles()

        // Start active loop
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_playState.value == PlayState.PLAYING) {
                runGameTick()
                delay(25) // ~40 FPS loop
            }
        }
    }

    private fun spawnInitialCollectibles() {
        val currentCollectibles = mutableListOf<Collectible>()
        for (i in 1..8) {
            currentCollectibles.add(
                Collectible(
                    id = idGenerator.incrementAndGet(),
                    isCoin = Random.nextBoolean(),
                    x = Random.nextFloat() * 800f + 100f,
                    y = Random.nextFloat() * 800f + 100f,
                    value = if (Random.nextBoolean()) 5f else 10f
                )
            )
        }
        collectibles.value = currentCollectibles
    }

    private fun calculateTalentEffects() {
        val upgrades = talentUpgrades.value
        
        // Damage multiplier (+25% per level)
        val dmgLvl = upgrades.firstOrNull { it.talentId == "DAMAGE" }?.level ?: 0
        talentDmgMultiplier = 1.0f + dmgLvl * 0.25f

        // Cooldown multiplier (-12% charge time per level)
        val cdLvl = upgrades.firstOrNull { it.talentId == "COOLDOWN" }?.level ?: 0
        talentCooldownMultiplier = 1.0f - cdLvl * 0.12f

        // Crit Chance (+8% base per level, starting at 5%)
        val critLvl = upgrades.firstOrNull { it.talentId == "CRIT" }?.level ?: 0
        talentCritChance = 0.05f + critLvl * 0.08f

        // Shield (Level determines charging speed)
        val shieldLvl = upgrades.firstOrNull { it.talentId == "SHIELD" }?.level ?: 0
        talentHasShield = shieldLvl > 0
        talentShieldLevel = shieldLvl

        // Magnet range (+30 range units per level)
        val magnetLvl = upgrades.firstOrNull { it.talentId == "MAGNET" }?.level ?: 0
        talentMagnetRange = 90f + magnetLvl * 35f

        // XP boost (+15% per level)
        val xpLvl = upgrades.firstOrNull { it.talentId == "XP_BOOST" }?.level ?: 0
        talentXpMultiplier = 1.0f + xpLvl * 0.15f
    }

    // Active screen tap slash
    fun executeActiveTapSlash(tx: Float, ty: Float) {
        if (_playState.value != PlayState.PLAYING) return

        val px = playerX.value
        val py = playerY.value
        
        // Calculate angle of attack
        val dx = tx - px
        val dy = ty - py
        val dist = sqrt(dx*dx + dy*dy)
        
        // We limit manual slash distance to player's range, but allow tapping anywhere to direct it
        val slashX = if (dist > 250f) px + (dx / dist) * 200f else tx
        val slashY = if (dist > 250f) py + (dy / dist) * 200f else ty

        // Apply skin sweep radius modifier
        val selectedSkin = gameProfile.value?.selectedSkin ?: "SIGMA_GLOW"
        val skinConfig = GameConfig.getSkin(selectedSkin)
        val radiusBase = if (selectedSkin == "GIGACHAD_CLEAVER") 170f else 130f
        
        // Trigger sound
        GameSound.playSlash()

        // Create visual slash
        val list = activeSlashes.value.toMutableList()
        list.add(SlashVisual(slashX, slashY, radiusBase, skinConfig.mainColor))
        activeSlashes.value = list

        // Check damage to glitches in this circle
        applySlashDamage(slashX, slashY, radiusBase, isManualTap = true)
    }

    private fun runGameTick() {
        gameTickCount++

        // 1. Move Player using joystick vector
        val speed = 8.5f
        if (joystickDx != 0f || joystickDy != 0f) {
            val nextX = (playerX.value + joystickDx * speed).coerceIn(40f, 960f)
            val nextY = (playerY.value + joystickDy * speed).coerceIn(40f, 960f)
            playerX.value = nextX
            playerY.value = nextY
        }

        // 2. Track game time in seconds
        if (gameTickCount % 40 == 0) {
            runTimeSeconds.value += 1
        }

        // 3. Spawning Glitches
        spawnTimerTicks++
        val spawnInterval = when {
            runTimeSeconds.value > 120 -> 25 // very fast swarm
            runTimeSeconds.value > 60 -> 45
            runTimeSeconds.value > 30 -> 65
            else -> 90 // starting spawn rate
        }
        if (spawnTimerTicks >= spawnInterval) {
            spawnTimerTicks = 0
            spawnGlitch()
        }

        // 4. Auto-Slash Cycle
        autoSlashCooldownTicks++
        val baseAutoSlashCd = 60 // ticks (~1.5s)
        val adjustedCd = (baseAutoSlashCd * talentCooldownMultiplier).toInt().coerceAtLeast(20)
        
        if (autoSlashCooldownTicks >= adjustedCd) {
            autoSlashCooldownTicks = 0
            executeAutoSlash()
        }

        // 5. Update active slashes
        val updatedSlashes = activeSlashes.value.map {
            it.copy(currentTick = it.currentTick + 1)
        }.filter { it.currentTick < it.maxDurationTicks }
        activeSlashes.value = updatedSlashes

        // 6. Move Glitches towards player
        val px = playerX.value
        val py = playerY.value
        val currentGlitches = glitches.value

        currentGlitches.forEach { glitch ->
            val dx = px - glitch.x
            val dy = py - glitch.y
            val dist = sqrt(dx*dx + dy*dy)
            
            if (dist > 5f) {
                // Add minor random drift to prevent stacking
                val driftX = (Random.nextFloat() - 0.5f) * 1.5f
                val driftY = (Random.nextFloat() - 0.5f) * 1.5f
                glitch.x += (dx / dist) * glitch.speed + driftX
                glitch.y += (dy / dist) * glitch.speed + driftY
            }

            // Hit flash tick reduction
            if (glitch.isHitFlash) {
                glitch.hitFlashTicks--
                if (glitch.hitFlashTicks <= 0) {
                    glitch.isHitFlash = false
                }
            }
        }

        // 7. Check glitch-to-player collision
        val remainingGlitches = mutableListOf<Glitch>()
        currentGlitches.forEach { glitch ->
            val dx = px - glitch.x
            val dy = py - glitch.y
            val dist = sqrt(dx*dx + dy*dy)
            val collisionDist = glitch.size + 24f // player radius is 24f

            if (dist < collisionDist) {
                // Collision! Damage player
                damagePlayer()
                
                // Explode glitch (with no points) so it doesn't instantly kill player
                spawnGlitchDebris(glitch.x, glitch.y, glitch.color, 12)
                
                // Spawn floating "-1 HP"
                spawnFloatingText(px, py - 30f, "-1 HP 💀", Color.Red)
            } else {
                remainingGlitches.add(glitch)
            }
        }
        glitches.value = remainingGlitches

        // 8. Shield Recharging
        if (talentHasShield && !runShieldActive.value) {
            val cooldownTicksMax = (600 - (talentShieldLevel * 80)).coerceAtLeast(200) // ~5-15s
            val currentCooldown = runShieldCooldownTicks.value + 1
            if (currentCooldown >= cooldownTicksMax) {
                runShieldActive.value = true
                runShieldCooldownTicks.value = 0
                spawnFloatingText(px, py - 40f, "AURA READY! 🛡️", Color(0xFF00FFCC))
                GameSound.playPowerUp()
            } else {
                runShieldCooldownTicks.value = currentCooldown
            }
        }

        // 9. Attract and Collect items (Magnet effect)
        val currentCollectibles = collectibles.value
        val remainingCollectibles = mutableListOf<Collectible>()
        
        currentCollectibles.forEach { item ->
            val dx = px - item.x
            val dy = py - item.y
            val dist = sqrt(dx*dx + dy*dy)

            if (dist < talentMagnetRange) {
                // Move item towards player
                val pullSpeed = 9f + (talentMagnetRange / (dist + 5f))
                item.x += (dx / dist) * pullSpeed
                item.y += (dy / dist) * pullSpeed
            }

            val collectionDist = 28f
            if (dist < collectionDist) {
                // Collect!
                collectItem(item)
            } else {
                remainingCollectibles.add(item)
            }
        }
        collectibles.value = remainingCollectibles

        // 10. Particles simulation
        val currentParticles = particles.value.map { p ->
            p.copy(
                x = p.x + p.vx,
                y = p.y + p.vy,
                alpha = p.alpha - 0.05f
            )
        }.filter { it.alpha > 0f }
        particles.value = currentParticles

        // 11. Floating numbers simulation
        val currentFloats = floatingNumbers.value.map { f ->
            f.copy(
                y = f.y - 1.8f,
                alpha = f.alpha - 0.04f
            )
        }.filter { it.alpha > 0f }
        floatingNumbers.value = currentFloats
    }

    private fun executeAutoSlash() {
        val px = playerX.value
        val py = playerY.value
        
        // Find closest enemy
        val enemyList = glitches.value
        if (enemyList.isEmpty()) return

        var closestEnemy: Glitch? = null
        var minDist = Float.MAX_VALUE
        enemyList.forEach { e ->
            val dx = e.x - px
            val dy = e.y - py
            val d = sqrt(dx*dx + dy*dy)
            if (d < minDist) {
                minDist = d
                closestEnemy = e
            }
        }

        closestEnemy?.let { enemy ->
            // Execute slash in direction of closest enemy
            val dx = enemy.x - px
            val dy = enemy.y - py
            val dist = sqrt(dx*dx + dy*dy)
            
            // Auto slash centers on a sweeping arc ahead of player
            val slashDist = if (dist > 160f) 120f else dist
            val slashX = px + (dx / dist) * slashDist
            val slashY = py + (dy / dist) * slashDist

            val selectedSkin = gameProfile.value?.selectedSkin ?: "SIGMA_GLOW"
            val skinConfig = GameConfig.getSkin(selectedSkin)
            val radiusBase = if (selectedSkin == "GIGACHAD_CLEAVER") 170f else 120f

            GameSound.playSlash()

            val list = activeSlashes.value.toMutableList()
            list.add(SlashVisual(slashX, slashY, radiusBase, skinConfig.accentColor))
            activeSlashes.value = list

            applySlashDamage(slashX, slashY, radiusBase, isManualTap = false)
        }
    }

    private fun applySlashDamage(sx: Float, sy: Float, radius: Float, isManualTap: Boolean) {
        val currentGlitches = glitches.value
        val remainingGlitches = mutableListOf<Glitch>()
        
        // Check skin effects
        val selectedSkin = gameProfile.value?.selectedSkin ?: "SIGMA_GLOW"
        val extraCritDmg = if (selectedSkin == "BASED_KATANA") 1.5f else 1.0f

        currentGlitches.forEach { glitch ->
            val dx = glitch.x - sx
            val dy = glitch.y - sy
            val dist = sqrt(dx*dx + dy*dy)
            
            if (dist < radius + glitch.size) {
                // HIT!
                // Determine if critical
                val isCrit = Random.nextFloat() < talentCritChance
                val baseDmg = 35f * talentDmgMultiplier
                val multiplier = if (isCrit) (2.0f * extraCritDmg) else 1.0f
                val damageDealt = baseDmg * multiplier

                glitch.hp -= damageDealt
                glitch.isHitFlash = true
                glitch.hitFlashTicks = 6

                // Knockback away from slash center
                val kx = glitch.x - sx
                val ky = glitch.y - sy
                val kdist = sqrt(kx*kx + ky*ky)
                if (kdist > 0.1f) {
                    glitch.x += (kx / kdist) * 45f
                    glitch.y += (ky / kdist) * 45f
                }

                // Play sound
                if (isCrit) {
                    GameSound.playCrit()
                }

                // Create damage float
                val dmgText = if (isCrit) "CRIT! ${damageDealt.toInt()}" else damageDealt.toInt().toString()
                val floatColor = if (isCrit) Color(0xFFFFCC00) else Color.White
                spawnFloatingText(glitch.x, glitch.y - 15f, dmgText, floatColor, isCrit = isCrit)

                // Spawn spark particles
                spawnGlitchDebris(glitch.x, glitch.y, glitch.color, 6)

                // Check death
                if (glitch.hp <= 0) {
                    // Explode!
                    spawnGlitchDebris(glitch.x, glitch.y, glitch.color, 16)
                    GameSound.playDamage()

                    // Record stats
                    runScore.value += 10

                    // Drop collectibles (Coin or XP)
                    dropLoot(glitch.x, glitch.y, glitch.xpReward, glitch.coinReward)
                } else {
                    remainingGlitches.add(glitch)
                }
            } else {
                remainingGlitches.add(glitch)
            }
        }
        glitches.value = remainingGlitches
    }

    private fun dropLoot(x: Float, y: Float, xpBase: Float, coinBase: Int) {
        val list = collectibles.value.toMutableList()
        val idGen = idGenerator
        
        // Always drop XP
        val finalXp = xpBase * talentXpMultiplier
        val selectedSkin = gameProfile.value?.selectedSkin ?: "SIGMA_GLOW"
        val xpDropCount = if (selectedSkin == "RAINBOW_SKIBIDI") 2 else 1

        for (i in 1..xpDropCount) {
            val ox = (Random.nextFloat() - 0.5f) * 40f
            val oy = (Random.nextFloat() - 0.5f) * 40f
            list.add(Collectible(idGen.incrementAndGet(), isCoin = false, x + ox, y + oy, finalXp))
        }

        // Percentage chance to drop coin
        var coinChance = 0.40f
        if (selectedSkin == "CAP_BUSTER") {
            coinChance += 0.15f
        }
        if (Random.nextFloat() < coinChance) {
            val ox = (Random.nextFloat() - 0.5f) * 40f
            val oy = (Random.nextFloat() - 0.5f) * 40f
            list.add(Collectible(idGen.incrementAndGet(), isCoin = true, x + ox, y + oy, coinBase.toFloat()))
        }

        collectibles.value = list
    }

    private fun collectItem(item: Collectible) {
        GameSound.playCoin()
        val px = playerX.value
        val py = playerY.value

        if (item.isCoin) {
            val coinsGained = item.value.toInt()
            runCoins.value += coinsGained
            runScore.value += 5
            spawnFloatingText(px, py - 30f, "+$coinsGained Coin 🪙", Color(0xFFFFD700))
        } else {
            val xpGained = item.value
            runScore.value += 2
            spawnFloatingText(px, py - 30f, "+${xpGained.toInt()} XP ✨", Color(0xFF00FFCC))
            addSessionXp(xpGained)
        }
    }

    private fun addSessionXp(amount: Float) {
        var currentXp = runXp.value + amount
        var required = runXpRequired.value

        while (currentXp >= required) {
            currentXp -= required
            runLevel.value += 1
            required = 100f + (runLevel.value - 1) * 50f
            
            // Heal player completely on level up
            runPlayerHp.value = runPlayerMaxHp.value
            
            // Splash of sparkles
            spawnGlitchDebris(playerX.value, playerY.value, Color(0xFF00FFCC), 40)
            spawnFloatingText(playerX.value, playerY.value - 50f, "LEVEL UP! 🚀", Color.Magenta, isCrit = true)
            
            GameSound.playLevelUp()
        }
        runXp.value = currentXp
        runXpRequired.value = required
    }

    private fun damagePlayer() {
        if (runShieldActive.value) {
            // Shield blocks damage
            runShieldActive.value = false
            runShieldCooldownTicks.value = 0
            GameSound.playShieldBreak()
            spawnFloatingText(playerX.value, playerY.value - 40f, "SHIELD BROKEN! 🛡️", Color(0xFFFF3333))
            return
        }

        val hp = runPlayerHp.value - 1
        runPlayerHp.value = hp
        GameSound.playDamage()

        // Severe visual screen shock (red particles around screen)
        spawnGlitchDebris(playerX.value, playerY.value, Color.Red, 25)

        if (hp <= 0) {
            endGameRun()
        }
    }

    private fun spawnGlitch() {
        val elapsed = runTimeSeconds.value
        
        // Random placement along screen boundary
        val edge = Random.nextInt(4) // 0: Top, 1: Right, 2: Bottom, 3: Left
        var sx = 0f
        var sy = 0f
        when (edge) {
            0 -> { sx = Random.nextFloat() * 1000f; sy = -40f }
            1 -> { sx = 1040f; sy = Random.nextFloat() * 1000f }
            2 -> { sx = Random.nextFloat() * 1000f; sy = 1040f }
            3 -> { sx = -40f; sy = Random.nextFloat() * 1000f }
        }

        // Determine glitch type by progress
        val type = when {
            elapsed > 90 -> {
                // End game high intensity: swarms of everything
                val roll = Random.nextFloat()
                when {
                    roll < 0.25f -> GlitchType.BRAIN_ROT
                    roll < 0.50f -> GlitchType.MATH_EXAM
                    roll < 0.70f -> GlitchType.DOOMSCROLL
                    roll < 0.85f -> GlitchType.FAKE_NEWS
                    else -> GlitchType.BOOMER
                }
            }
            elapsed > 45 -> {
                // Mid game: Doomscrollers, exams, brain rot starts
                val roll = Random.nextFloat()
                when {
                    roll < 0.35f -> GlitchType.DOOMSCROLL
                    roll < 0.65f -> GlitchType.MATH_EXAM
                    roll < 0.85f -> GlitchType.FAKE_NEWS
                    else -> GlitchType.BOOMER
                }
            }
            else -> {
                // Starting game: Boomers and fake news
                if (Random.nextFloat() < 0.3f) GlitchType.FAKE_NEWS else GlitchType.BOOMER
            }
        }

        val glitch = when (type) {
            GlitchType.BOOMER -> Glitch(
                id = idGenerator.incrementAndGet(),
                type = GlitchType.BOOMER,
                name = "Boomer Advice",
                phrase = phrasesBoomer.random(),
                x = sx, y = sy,
                speed = 2.4f,
                hp = 100f, maxHp = 100f,
                size = 35f,
                color = Color(0xFFFFA500), // Orange
                xpReward = 15f,
                coinReward = 3
            )
            GlitchType.DOOMSCROLL -> Glitch(
                id = idGenerator.incrementAndGet(),
                type = GlitchType.DOOMSCROLL,
                name = "Doomscroll Loop",
                phrase = phrasesDoomscroll.random(),
                x = sx, y = sy,
                speed = 3.6f,
                hp = 60f, maxHp = 60f,
                size = 28f,
                color = Color(0xFFFF3366), // Hot Crimson
                xpReward = 20f,
                coinReward = 5
            )
            GlitchType.MATH_EXAM -> Glitch(
                id = idGenerator.incrementAndGet(),
                type = GlitchType.MATH_EXAM,
                name = "Unc Calculus",
                phrase = phrasesMathExam.random(),
                x = sx, y = sy,
                speed = 5.2f,
                hp = 30f, maxHp = 30f,
                size = 22f,
                color = Color(0xFF33CCFF), // Bright Cyan
                xpReward = 12f,
                coinReward = 2
            )
            GlitchType.BRAIN_ROT -> Glitch(
                id = idGenerator.incrementAndGet(),
                type = GlitchType.BRAIN_ROT,
                name = "Brain Rot Swarm",
                phrase = phrasesBrainRot.random(),
                x = sx, y = sy,
                speed = 4.4f,
                hp = 80f, maxHp = 80f,
                size = 30f,
                color = Color(0xFFCC33FF), // Neon Purple
                xpReward = 25f,
                coinReward = 8
            )
            GlitchType.FAKE_NEWS -> Glitch(
                id = idGenerator.incrementAndGet(),
                type = GlitchType.FAKE_NEWS,
                name = "Fake News Leak",
                phrase = phrasesFakeNews.random(),
                x = sx, y = sy,
                speed = 3.1f,
                hp = 50f, maxHp = 50f,
                size = 26f,
                color = Color(0xFFFFFF33), // Toxic Yellow
                xpReward = 18f,
                coinReward = 12 // Drops high coins!
            )
        }

        val list = glitches.value.toMutableList()
        list.add(glitch)
        glitches.value = list
    }

    private fun spawnGlitchDebris(x: Float, y: Float, color: Color, count: Int) {
        val list = particles.value.toMutableList()
        val idGen = idGenerator
        for (i in 1..count) {
            val angle = Random.nextFloat() * 2.0 * Math.PI
            val force = Random.nextFloat() * 8f + 2f
            list.add(
                Particle(
                    id = idGen.incrementAndGet(),
                    x = x,
                    y = y,
                    vx = (cos(angle) * force).toFloat(),
                    vy = (sin(angle) * force).toFloat(),
                    color = color,
                    alpha = 1.0f,
                    size = Random.nextFloat() * 6f * dpToPx() + 3f * dpToPx()
                )
            )
        }
        particles.value = list
    }

    private fun spawnFloatingText(x: Float, y: Float, text: String, color: Color, isCrit: Boolean = false) {
        val list = floatingNumbers.value.toMutableList()
        list.add(
            FloatingNumber(
                id = idGenerator.incrementAndGet(),
                x = x,
                y = y,
                text = text,
                color = color,
                alpha = 1.0f,
                isCrit = isCrit
            )
        )
        floatingNumbers.value = list
    }

    private fun dpToPx() = 2.5f // rough scale factor inside our canvas viewport

    private fun endGameRun() {
        _playState.value = PlayState.GAME_OVER
        gameLoopJob?.cancel()
        gameLoopJob = null

        // Calculate and save metrics to career profile
        val finalCoins = runCoins.value
        val finalKills = runScore.value / 10 // rough approximation
        val finalXp = runScore.value * 0.4f // conversion
        val finalSeconds = runTimeSeconds.value

        viewModelScope.launch {
            repository.saveGameSession(
                coinsEarned = finalCoins,
                xpEarned = finalXp,
                killsInSession = finalKills,
                timeSurvivedSeconds = finalSeconds
            )
        }
    }

    // Careers Purchase Methods
    fun buySkin(skinId: String) {
        val config = GameConfig.getSkin(skinId)
        viewModelScope.launch {
            val success = repository.purchaseSkin(skinId, config.cost)
            if (success) {
                repository.equipSkin(skinId)
                calculateTalentEffects() // re-calc state
                GameSound.playLevelUp()
            } else {
                GameSound.playDamage() // buzz error sound
            }
        }
    }

    fun buyTalent(talentId: String) {
        val config = GameConfig.getTalent(talentId) ?: return
        val currentUpgrades = talentUpgrades.value
        val currentLvl = currentUpgrades.firstOrNull { it.talentId == talentId }?.level ?: 0
        
        if (currentLvl >= config.maxLevel) return

        val cost = (config.baseCost * Math.pow(config.costMultiplier.toDouble(), currentLvl.toDouble())).toInt()

        viewModelScope.launch {
            val success = repository.purchaseTalent(talentId, cost)
            if (success) {
                calculateTalentEffects()
                GameSound.playLevelUp()
            } else {
                GameSound.playDamage() // error sfx
            }
        }
    }

    fun equipSkin(skinId: String) {
        viewModelScope.launch {
            repository.equipSkin(skinId)
            calculateTalentEffects()
        }
    }

    fun equipBadge(badgeId: String) {
        viewModelScope.launch {
            repository.equipBadge(badgeId)
        }
    }
}
