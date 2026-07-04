package com.example.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.GameProfile
import com.example.data.model.GameConfig
import com.example.data.model.SkinConfig
import com.example.data.model.TalentConfig
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val playState by viewModel.playState.collectAsStateWithLifecycle()
    val profile by viewModel.gameProfile.collectAsStateWithLifecycle()
    val unlockedSkins by viewModel.unlockedSkins.collectAsStateWithLifecycle()
    val unlockedBadges by viewModel.unlockedBadges.collectAsStateWithLifecycle()
    val talentUpgrades by viewModel.talentUpgrades.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (playState) {
            PlayState.IDLE -> {
                LobbyScreen(
                    profile = profile ?: GameProfile(),
                    unlockedSkins = unlockedSkins.map { it.skinId },
                    unlockedBadges = unlockedBadges.map { it.badgeId },
                    talentUpgrades = talentUpgrades.associate { it.talentId to it.level },
                    onStartGame = { viewModel.startGame() },
                    onEquipSkin = { viewModel.equipSkin(it) },
                    onEquipBadge = { viewModel.equipBadge(it) },
                    onBuySkin = { viewModel.buySkin(it) },
                    onBuyTalent = { viewModel.buyTalent(it) }
                )
            }
            PlayState.PLAYING -> {
                ArenaScreen(viewModel = viewModel)
            }
            PlayState.GAME_OVER -> {
                GameOverScreen(
                    viewModel = viewModel,
                    profile = profile ?: GameProfile()
                )
            }
        }
    }
}

@Composable
fun LobbyScreen(
    profile: GameProfile,
    unlockedSkins: List<String>,
    unlockedBadges: List<String>,
    talentUpgrades: Map<String, Int>,
    onStartGame: () -> Unit,
    onEquipSkin: (String) -> Unit,
    onEquipBadge: (String) -> Unit,
    onBuySkin: (String) -> Unit,
    onBuyTalent: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: HOME, 1: TALENTS, 2: SKINS, 3: BADGES
    val equippedSkin = profile.selectedSkin
    val equippedBadge = profile.selectedBadge

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // 1. Header: Profile & Currency
        HeaderSection(profile = profile)

        // 2. Main Content Area (displays active screen based on selectedTab)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> HomeDashboard(
                    profile = profile,
                    onStartGame = onStartGame,
                    onNavigateToSkins = { selectedTab = 2 }
                )
                1 -> Box(modifier = Modifier.padding(16.dp)) {
                    TalentsTab(
                        coins = profile.coins,
                        upgrades = talentUpgrades,
                        onBuyTalent = onBuyTalent
                    )
                }
                2 -> Box(modifier = Modifier.padding(16.dp)) {
                    SkinsTab(
                        coins = profile.coins,
                        equipped = equippedSkin,
                        unlocked = unlockedSkins,
                        onEquip = onEquipSkin,
                        onBuy = onBuySkin
                    )
                }
                3 -> Box(modifier = Modifier.padding(16.dp)) {
                    BadgesTab(
                        equipped = equippedBadge,
                        unlocked = unlockedBadges,
                        onEquip = onEquipBadge
                    )
                }
            }
        }

        // 3. Bottom Nav Bar
        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

@Composable
fun HeaderSection(profile: GameProfile) {
    val equippedBadge = profile.selectedBadge
    val badgeObj = GameConfig.getBadge(equippedBadge)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDarkCard)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + Level/Name info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with Purple/Lavender Gradient border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(CyberPurple, CyberPink)),
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .background(CyberBlack, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeObj.emoji, // Display badge emoji as cool avatar!
                    fontSize = 20.sp
                )
            }

            Column {
                Text(
                    text = "LEVEL ${profile.level}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = CyberPink
                )
                Text(
                    text = badgeObj.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = CyberWhite
                )
            }
        }

        // Currency Metrics (High Density style px-3 py-1.5 rounded-full border border-slate-700)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Coins Counter
            Row(
                modifier = Modifier
                    .background(Color(0xFF2A2A2E), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = profile.coins.toString(),
                    color = CyberYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer(rotationZ = 45f)
                        .background(CyberYellow, RoundedCornerShape(2.dp))
                )
            }

            // High Score metric
            Row(
                modifier = Modifier
                    .background(Color(0xFF2A2A2E), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = profile.highScore.toString(),
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(CyberCyan, CircleShape)
                )
            }
        }
    }
}

@Composable
fun HomeDashboard(
    profile: GameProfile,
    onStartGame: () -> Unit,
    onNavigateToSkins: () -> Unit
) {
    val skinObj = GameConfig.getSkin(profile.selectedSkin)
    val badgeObj = GameConfig.getBadge(profile.selectedBadge)

    // Dynamic Rank calculation based on High Score
    val rankLabel: String
    val rankAbbr: String
    val rankColor: Color
    when {
        profile.highScore < 10 -> {
            rankLabel = "Bronze III"
            rankAbbr = "B3"
            rankColor = Color(0xFFCD7F32)
        }
        profile.highScore in 10..24 -> {
            rankLabel = "Silver I"
            rankAbbr = "S1"
            rankColor = Color(0xFFC0C0C0)
        }
        profile.highScore in 25..49 -> {
            rankLabel = "Gold II"
            rankAbbr = "G2"
            rankColor = CyberYellow
        }
        else -> {
            rankLabel = "Diamond II"
            rankAbbr = "D2"
            rankColor = CyberPink
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Character & active status container (Main Viewport)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 1. Rank Badge Overlay & Friends Box (Left-aligned)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rank Box
                Box(
                    modifier = Modifier
                        .background(Color(0xCC2A2A2E), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "RANKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGrey
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Small rotating badge diamond
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(rotationZ = 45f)
                                    .background(rankColor, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rankAbbr,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyberBlack,
                                    modifier = Modifier.graphicsLayer(rotationZ = -45f)
                                )
                            }
                            Text(
                                text = rankLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = CyberWhite
                            )
                        }
                    }
                }

                // Friends Box
                Box(
                    modifier = Modifier
                        .background(Color(0xCC2A2A2E), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "FRIENDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGrey
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(CyberGreen, CircleShape)
                            )
                            Text(
                                text = "14 Online",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = CyberWhite
                            )
                        }
                    }
                }
            }

            // 2. Right Side Floating Menu
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val buttonModifier = Modifier
                    .size(48.dp)
                    .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))

                IconButton(
                    onClick = onNavigateToSkins,
                    modifier = buttonModifier
                ) {
                    Text("🎒", fontSize = 18.sp)
                }
                IconButton(
                    onClick = onNavigateToSkins,
                    modifier = buttonModifier
                ) {
                    Text("🛒", fontSize = 18.sp)
                }
                Box(
                    modifier = buttonModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏆", fontSize = 18.sp)
                }
            }

            // 3. Center Character Showcase (glowing with pulsing animation)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Pulsing Glow behind hero avatar
                    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .background(
                                Brush.radialGradient(
                                    listOf(skinObj.mainColor.copy(0.35f), Color.Transparent)
                                )
                            )
                    )

                    // Hero character central symbol/orb
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(skinObj.mainColor, skinObj.accentColor)
                                )
                            )
                            .border(3.dp, CyberWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚔️",
                            fontSize = 44.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CURRENT HERO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    ),
                    color = CyberPink
                )
                Text(
                    text = skinObj.name.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    ),
                    color = CyberWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats/Metrics Overlay row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "WEAPON",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberGrey
                            )
                            Text(
                                text = badgeObj.emoji + " " + badgeObj.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = CyberWhite,
                                maxLines = 1
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "EFFECT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberGrey
                            )
                            Text(
                                text = skinObj.effectDesc,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = skinObj.mainColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Bottom CTA & Progression Block (Season + Play button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDarkCard, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Battle Pass progress
                Column {
                    val passTier = profile.totalKills / 10 + 1
                    val passProgress = (profile.totalKills % 10) / 10f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "SEASON 1: GLITCH CITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic
                            ),
                            color = CyberWhite
                        )
                        Text(
                            text = "Battle Pass Tier $passTier/100",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGrey
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Premium custom progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222530))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(passProgress.coerceAtLeast(0.05f))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CyberPurple, CyberBlue)
                                    )
                                )
                        )
                    }
                }

                // Match Selection and Big Play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Match Type card
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MATCH TYPE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGrey,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A2A2E), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RANKED ARENA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyberWhite
                                )
                                Text("⚡", fontSize = 11.sp, color = CyberPink)
                            }
                        }
                    }

                    // Play Button with pulsing transition
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_play")
                    val buttonScale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "btnScale"
                    )

                    Button(
                        onClick = onStartGame,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                            .testTag("start_game_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(CyberPink, CyberPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PLAY NOW",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 1.5.sp
                                ),
                                color = CyberBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = CyberBlack,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp, Color(0x13FFFFFF), RoundedCornerShape(0.dp)
            )
    ) {
        val navItems = listOf(
            Triple(0, "🏠", "Home"),
            Triple(1, "⚡", "Talents"),
            Triple(2, "⚔️", "Skins"),
            Triple(3, "🗿", "Badges")
        )

        navItems.forEach { (index, emoji, label) ->
            val isSelected = selectedTab == index
            val tintColor = if (isSelected) CyberPink else CyberGrey

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            color = tintColor
                        )
                        Text(
                            text = label.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = tintColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun TalentsTab(coins: Int, upgrades: Map<String, Int>, onBuyTalent: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GameConfig.TALENTS) { talent ->
            val lvl = upgrades[talent.id] ?: 0
            val isMax = lvl >= talent.maxLevel
            
            val cost = if (isMax) 0 else {
                (talent.baseCost * Math.pow(talent.costMultiplier.toDouble(), lvl.toDouble())).toInt()
            }
            val canAfford = coins >= cost && !isMax

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (canAfford) CyberCyan.copy(0.4f) else CyberCardBorder,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(talent.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(talent.name, fontWeight = FontWeight.Bold, color = CyberWhite)
                        Text(talent.description, fontSize = 11.sp, color = CyberGrey)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Mini level meter
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 1..talent.maxLevel) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 6.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (i <= lvl) CyberCyan else CyberCardBorder
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lvl $lvl/${talent.maxLevel}", fontSize = 10.sp, color = CyberGrey)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isMax) {
                        Text("MAXED", color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Button(
                            onClick = { onBuyTalent(talent.id) },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) CyberCyan else CyberCardBorder,
                                disabledContainerColor = CyberCardBorder
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("UPGRADE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (canAfford) CyberBlack else CyberGrey)
                                Text("🪙 $cost", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (canAfford) CyberBlack else CyberGrey)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkinsTab(
    coins: Int,
    equipped: String,
    unlocked: List<String>,
    onEquip: (String) -> Unit,
    onBuy: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GameConfig.SKINS) { skin ->
            val isUnlocked = unlocked.contains(skin.id)
            val isEquipped = equipped == skin.id
            val canAfford = coins >= skin.cost

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isEquipped) skin.mainColor else CyberCardBorder,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview Circle representing the glow
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(skin.mainColor, skin.accentColor)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(skin.name, fontWeight = FontWeight.Bold, color = CyberWhite)
                        Text(skin.description, fontSize = 11.sp, color = CyberGrey)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = skin.effectDesc,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = skin.mainColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    when {
                        isEquipped -> {
                            Text("EQUIPPED", color = skin.mainColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        isUnlocked -> {
                            Button(
                                onClick = { onEquip(skin.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("EQUIP", fontSize = 11.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { onBuy(skin.id) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAfford) CyberCyan else CyberCardBorder,
                                    disabledContainerColor = CyberCardBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("GET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (canAfford) CyberBlack else CyberGrey)
                                    Text("🪙 ${skin.cost}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (canAfford) CyberBlack else CyberGrey)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgesTab(equipped: String, unlocked: List<String>, onEquip: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GameConfig.BADGES) { badge ->
            val isUnlocked = unlocked.contains(badge.id)
            val isEquipped = equipped == badge.id

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) CyberDarkCard else CyberDarkCard.copy(0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isEquipped) CyberCyan else CyberCardBorder,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) CyberBlack else CyberCardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUnlocked) badge.emoji else "🔒",
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = badge.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) CyberWhite else CyberGrey
                            )
                            if (!isUnlocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(LOCKED)", fontSize = 9.sp, color = CyberPink, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(badge.description, fontSize = 11.sp, color = CyberGrey)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "To Unlock: ${badge.requirement}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUnlocked) CyberCyan.copy(0.8f) else CyberGrey
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isUnlocked) {
                        if (isEquipped) {
                            Text("EQUIPPED", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        } else {
                            Button(
                                onClick = { onEquip(badge.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SHOW", fontSize = 11.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaScreen(viewModel: GameViewModel) {
    val px by viewModel.playerX.collectAsStateWithLifecycle()
    val py by viewModel.playerY.collectAsStateWithLifecycle()
    val score by viewModel.runScore.collectAsStateWithLifecycle()
    val coins by viewModel.runCoins.collectAsStateWithLifecycle()
    val hp by viewModel.runPlayerHp.collectAsStateWithLifecycle()
    val level by viewModel.runLevel.collectAsStateWithLifecycle()
    val xp by viewModel.runXp.collectAsStateWithLifecycle()
    val xpRequired by viewModel.runXpRequired.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.runTimeSeconds.collectAsStateWithLifecycle()
    val isShieldActive by viewModel.runShieldActive.collectAsStateWithLifecycle()

    val glitches by viewModel.glitches.collectAsStateWithLifecycle()
    val collectibles by viewModel.collectibles.collectAsStateWithLifecycle()
    val particles by viewModel.particles.collectAsStateWithLifecycle()
    val floats by viewModel.floatingNumbers.collectAsStateWithLifecycle()
    val slashes by viewModel.activeSlashes.collectAsStateWithLifecycle()

    // Retrieve active skin
    val profile by viewModel.gameProfile.collectAsStateWithLifecycle()
    val activeSkin = profile?.selectedSkin ?: "SIGMA_GLOW"
    val skinConfig = GameConfig.getSkin(activeSkin)

    // Layout configuration
    var canvasWidth by remember { mutableStateOf(1f) }
    var canvasHeight by remember { mutableStateOf(1f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen drawing canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Convert physical tap coordinates to virtual arena coordinates (0..1000)
                        val scaleX = 1000f / canvasWidth
                        val scaleY = 1000f / canvasHeight
                        viewModel.executeActiveTapSlash(
                            tx = offset.x * scaleX,
                            ty = offset.y * scaleY
                        )
                    }
                }
                .testTag("battle_canvas")
        ) {
            canvasWidth = size.width
            canvasHeight = size.height

            val scaleX = size.width / 1000f
            val scaleY = size.height / 1000f

            // Draw Background moving lines based on player coordinates (moving camera look)
            val gridSpacing = 80f * scaleX
            val cameraOffsetX = -(px % 80f) * scaleX
            val cameraOffsetY = -(py % 80f) * scaleY

            // Draw horizontal lines
            var yLine = cameraOffsetY
            while (yLine < size.height) {
                drawLine(
                    color = Color(0xFF141724),
                    start = Offset(0f, yLine),
                    end = Offset(size.width, yLine),
                    strokeWidth = 1.dp.toPx()
                )
                yLine += gridSpacing
            }

            // Draw vertical lines
            var xLine = cameraOffsetX
            while (xLine < size.width) {
                drawLine(
                    color = Color(0xFF141724),
                    start = Offset(xLine, 0f),
                    end = Offset(xLine, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                xLine += gridSpacing
            }

            // 1. Draw Collectibles
            collectibles.forEach { item ->
                val ix = item.x * scaleX
                val iy = item.y * scaleY
                if (item.isCoin) {
                    // Golden vibe coin
                    drawCircle(Color(0xFFFFD700), radius = 8.dp.toPx(), center = Offset(ix, iy))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(ix, iy), style = Stroke(1.5.dp.toPx()))
                } else {
                    // Cyan XP crystal
                    drawCircle(Color(0xFF00FFCC), radius = 6.dp.toPx(), center = Offset(ix, iy))
                    drawCircle(Color(0xFF9900FF), radius = 3.dp.toPx(), center = Offset(ix, iy))
                }
            }

            // 2. Draw Active Slashes
            slashes.forEach { slash ->
                val sx = slash.x * scaleX
                val sy = slash.y * scaleY
                val progress = slash.currentTick.toFloat() / slash.maxDurationTicks
                val radiusPx = slash.radius * scaleX * progress
                val alpha = 1.0f - progress

                drawCircle(
                    color = slash.color.copy(alpha = alpha * 0.15f),
                    radius = radiusPx,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = slash.color.copy(alpha = alpha * 0.8f),
                    radius = radiusPx,
                    center = Offset(sx, sy),
                    style = Stroke(3.dp.toPx())
                )
            }

            // 3. Draw Glitches
            glitches.forEach { glitch ->
                val gx = glitch.x * scaleX
                val gy = glitch.y * scaleY
                val rPx = glitch.size * scaleX

                // Main body
                val bodyColor = if (glitch.isHitFlash) Color.White else glitch.color
                drawCircle(bodyColor, radius = rPx, center = Offset(gx, gy))
                
                // Holographic outline ring
                drawCircle(
                    color = glitch.color.copy(alpha = 0.5f),
                    radius = rPx + 4.dp.toPx(),
                    center = Offset(gx, gy),
                    style = Stroke(1.5.dp.toPx())
                )

                // Render funny glitch labels & phrase
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10.sp.toPx()
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                
                // Draw name tag
                drawContext.canvas.nativeCanvas.drawText(
                    glitch.name,
                    gx,
                    gy - rPx - 16.dp.toPx(),
                    textPaint
                )

                // Draw humorous phrase
                val phrasePaint = Paint().apply {
                    color = glitch.color.toArgb()
                    textSize = 9.sp.toPx()
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "\"${glitch.phrase}\"",
                    gx,
                    gy - rPx - 4.dp.toPx(),
                    phrasePaint
                )
            }

            // 4. Draw Particles (Debris)
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(p.x * scaleX, p.y * scaleY)
                )
            }

            // 5. Draw Player Avatar (Neon blade master)
            val pXp = px * scaleX
            val pYp = py * scaleY
            val radiusPlayer = 24f * scaleX

            // Outer pulse glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(skinConfig.mainColor.copy(0.4f), Color.Transparent),
                    center = Offset(pXp, pYp),
                    radius = radiusPlayer * 3.5f
                ),
                radius = radiusPlayer * 3.5f,
                center = Offset(pXp, pYp)
            )

            // Outer metallic rim
            drawCircle(
                color = skinConfig.accentColor,
                radius = radiusPlayer,
                center = Offset(pXp, pYp),
                style = Stroke(3.dp.toPx())
            )

            // Inner core
            drawCircle(
                color = Color.White,
                radius = radiusPlayer * 0.6f,
                center = Offset(pXp, pYp)
            )

            // Draw spinning orbit shield if active
            if (isShieldActive) {
                val cycleFraction = (System.currentTimeMillis() % 2000) / 2000f
                val orbitAngle = cycleFraction * 2.0 * Math.PI
                val orbitRadius = radiusPlayer * 2.2f
                val sx = pXp + (cos(orbitAngle) * orbitRadius).toFloat()
                val sy = pYp + (sin(orbitAngle) * orbitRadius).toFloat()

                // Protective shield orbit line
                drawCircle(
                    color = Color(0xFF00FFCC).copy(0.2f),
                    radius = orbitRadius,
                    center = Offset(pXp, pYp),
                    style = Stroke(1.dp.toPx())
                )

                // Shield bubble node
                drawCircle(
                    color = Color(0xFF00FFCC),
                    radius = 8.dp.toPx(),
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }

            // 6. Draw Floating damage text
            floats.forEach { f ->
                val fPaint = Paint().apply {
                    color = f.color.copy(alpha = f.alpha).toArgb()
                    textSize = if (f.isCrit) 16.sp.toPx() else 12.sp.toPx()
                    typeface = if (f.isCrit) Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC) else Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    f.text,
                    f.x * scaleX,
                    f.y * scaleY,
                    fPaint
                )
            }
        }

        // Top Overlay Header: Live stats & XP Progress
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score & Time elapsed
                Column {
                    Text(
                        text = "ELIMINATED: $score",
                        color = CyberWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    val timerStr = String.format("%02d:%02d", mins, secs)
                    Text(
                        text = "SURVIVED: $timerStr",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Current Health Display (Hearts)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart",
                            tint = if (i <= hp) CyberPink else CyberCardBorder,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // XP and level bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lvl $level",
                    color = CyberWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                val xpFraction = (xp / xpRequired).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(CyberCardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xpFraction)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CyberCyan, CyberPurple)
                                )
                            )
                    )
                }

                Text(
                    text = "🪙 $coins",
                    color = CyberYellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap anywhere to actively Cyber-Slash! ⚡",
                color = CyberGrey,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Joystick overlay in the bottom container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
        ) {
            VirtualJoystick(
                modifier = Modifier.align(Alignment.BottomStart),
                onVectorChange = { dx, dy ->
                    viewModel.setJoystickVector(dx, dy)
                }
            )
        }
    }
}

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onVectorChange: (Float, Float) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 60.dp

    Box(
        modifier = modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(Color(0x1A00FFCC))
            .border(2.dp, Color(0x3300FFCC), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        onVectorChange(0f, 0f)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onVectorChange(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val distance = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val maxRadiusPx = maxRadius.toPx()

                        dragOffset = if (distance > maxRadiusPx) {
                            Offset(
                                (newOffset.x / distance) * maxRadiusPx,
                                (newOffset.y / distance) * maxRadiusPx
                            )
                        } else {
                            newOffset
                        }

                        // Normalize vector to -1.0 .. 1.0 range
                        val normX = dragOffset.x / maxRadiusPx
                        val normY = dragOffset.y / maxRadiusPx
                        onVectorChange(normX, normY)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Center pad
        Box(
            modifier = Modifier
                .offset(
                    x = (dragOffset.x / LocalContext.current.resources.displayMetrics.density).dp,
                    y = (dragOffset.y / LocalContext.current.resources.displayMetrics.density).dp
                )
                .size(54.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CyberCyan, Color(0xFF009977))
                    )
                )
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun GameOverScreen(viewModel: GameViewModel, profile: GameProfile) {
    val score by viewModel.runScore.collectAsStateWithLifecycle()
    val coinsGained by viewModel.runCoins.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.runTimeSeconds.collectAsStateWithLifecycle()

    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timerStr = String.format("%02d:%02d", mins, secs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GLITCH OVERLOAD",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                letterSpacing = 2.sp
            ),
            color = CyberPink,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your brain cells collapsed into infinite scrolling.",
            color = CyberGrey,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Holo-recap stats sheet
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyberPink.copy(0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RUN STATISTICS",
                    fontWeight = FontWeight.Black,
                    color = CyberWhite,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                StatLine(emoji = "💀", label = "Glitches Cleared", value = (score / 10).toString())
                StatLine(emoji = "⏱️", label = "Time Survived", value = timerStr)
                StatLine(emoji = "🪙", label = "Vibe Coins Gained", value = "+$coinsGained", valueColor = CyberYellow)
                StatLine(emoji = "✨", label = "Total Score Boost", value = score.toString(), valueColor = CyberCyan)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Fun performance title
                val performanceTitle = when {
                    elapsedSeconds >= 120 -> "🗿 GigaChad Mindset"
                    elapsedSeconds >= 60 -> "🔥 Based Slasher"
                    elapsedSeconds >= 30 -> "🐣 Cap Detector"
                    else -> "💀 Brainrot Victim"
                }
                Text(
                    text = "STATUS: $performanceTitle",
                    fontWeight = FontWeight.Black,
                    color = CyberPink,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Actions
        Button(
            onClick = { viewModel.startGame() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("run_it_back_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "RUN IT BACK 🚀",
                color = CyberBlack,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.goToLobby() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                .testTag("return_lobby_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "RETURN TO LOBBY 🏠",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun StatLine(emoji: String, label: String, value: String, valueColor: Color = CyberWhite) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            Text(label, color = CyberGrey, fontSize = 13.sp)
        }
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// Extension to color conversion for canvas painting
fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
            ((this.red * 255.0f + 0.5f).toInt() shl 16) or
            ((this.green * 255.0f + 0.5f).toInt() shl 8) or
            (this.blue * 255.0f + 0.5f).toInt()
}
