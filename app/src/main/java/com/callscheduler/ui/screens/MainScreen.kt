package com.callscheduler.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.RepeatMode as ComposeRepeatMode
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

import com.callscheduler.data.model.*
import com.callscheduler.data.model.RepeatMode
import com.callscheduler.ui.*
import com.callscheduler.ui.theme.AppColors

// ─────────────────────────────────────────────
// ÉCRAN PRINCIPAL
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onToggleCall: (ScheduledCall) -> Unit,
    onDeleteCall: (ScheduledCall) -> Unit,
    onAddCall: () -> Unit,
    onEditCall: (ScheduledCall) -> Unit,
    onDuplicateCall: (ScheduledCall) -> Unit,
    onFilterChanged: (FilterMode) -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onSearchChanged: (String) -> Unit,
    onToggleStats: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptic = LocalHapticFeedback.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.SpaceBlack,
        topBar = {
            TopBar(
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                onToggleStats = onToggleStats,
                onSearchChanged = onSearchChanged,
                onFilterChanged = onFilterChanged,
                onSortChanged = onSortChanged
            )
        },
        floatingActionButton = {
            PulseFab(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddCall()
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = uiState.showStatsPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                StatsPanel(uiState)
            }

            val todayCalls = remember(uiState.calls, now) {
                uiState.calls.filter {
                    it.isEnabled && it.nextCallTimestamp > now &&
                    it.nextCallTimestamp < now + 86_400_000
                }.sortedBy { it.nextCallTimestamp }
            }

            if (todayCalls.isNotEmpty()) {
                NextCallBanner(todayCalls.first(), now)
            }

            if (uiState.isLoading) {
                LoadingScreen()
            } else if (uiState.calls.isEmpty()) {
                EmptyScreen(onAddCall)
            } else {
                CallList(
                    calls = uiState.calls,
                    onToggle = onToggleCall,
                    onDelete = onDeleteCall,
                    onEdit = onEditCall,
                    onDuplicate = onDuplicateCall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    uiState: UiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onToggleStats: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (FilterMode) -> Unit,
    onSortChanged: (SortMode) -> Unit
) {
    var searchExpanded by remember(uiState.searchQuery) {
        mutableStateOf(uiState.searchQuery.isNotEmpty())
    }
    var showFilterMenu by remember { mutableStateOf(false) }

    LargeTopAppBar(
        title = {
            if (searchExpanded) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Rechercher...", color = AppColors.TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Gold,
                        unfocusedBorderColor = AppColors.Divider,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        cursorColor = AppColors.Gold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { searchExpanded = false; onSearchChanged("") }) {
                            Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary)
                        }
                    }
                )
            } else {
                Column {
                    Text(
                        "Planificateur",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${uiState.enabledCount} actif${if (uiState.enabledCount > 1) "s" else ""} · ${uiState.calls.size} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Gold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = AppColors.SpaceBlack,
            scrolledContainerColor = AppColors.SurfaceDark,
            titleContentColor = AppColors.TextPrimary
        ),
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = onToggleStats) {
                Icon(
                    if (uiState.showStatsPanel) Icons.Default.BarChart else Icons.Outlined.BarChart,
                    contentDescription = "Stats",
                    tint = if (uiState.showStatsPanel) AppColors.Gold else AppColors.TextSecondary
                )
            }
            IconButton(onClick = { searchExpanded = true }) {
                Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = AppColors.TextSecondary)
            }
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtrer", tint = AppColors.TextSecondary)
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(AppColors.CardDark)
                ) {
                    Text(
                        "FILTRER",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Gold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    FilterMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label, color = if (uiState.selectedFilter == mode) AppColors.Gold else AppColors.TextPrimary) },
                            onClick = { onFilterChanged(mode); showFilterMenu = false },
                            leadingIcon = {
                                if (uiState.selectedFilter == mode)
                                    Icon(Icons.Default.Check, null, tint = AppColors.Gold, modifier = Modifier.size(16.dp))
                                else Spacer(Modifier.size(16.dp))
                            }
                        )
                    }
                    Divider(color = AppColors.Divider)
                    Text(
                        "TRIER",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Gold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label, color = if (uiState.sortMode == mode) AppColors.Gold else AppColors.TextPrimary) },
                            onClick = { onSortChanged(mode); showFilterMenu = false },
                            leadingIcon = {
                                if (uiState.sortMode == mode)
                                    Icon(Icons.Default.Check, null, tint = AppColors.Gold, modifier = Modifier.size(16.dp))
                                else Spacer(Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }
    )
}

val FilterMode.label get() = when(this) {
    FilterMode.ALL -> "Tous les appels"
    FilterMode.ENABLED_ONLY -> "Actifs seulement"
    FilterMode.TODAY -> "Aujourd'hui"
    FilterMode.BY_GROUP -> "Par groupe"
}

val SortMode.label get() = when(this) {
    SortMode.BY_TIME -> "Par heure"
    SortMode.BY_NAME -> "Par nom"
    SortMode.BY_DATE -> "Par date"
    SortMode.BY_STATUS -> "Par statut"
}

// ─────────────────────────────────────────────
// BANNIÈRE PROCHAIN APPEL
// ─────────────────────────────────────────────

@Composable
fun NextCallBanner(call: ScheduledCall, now: Long) {
    val diff = call.nextCallTimestamp - now
    val hours = diff / 3_600_000
    val minutes = (diff % 3_600_000) / 60_000

    val infiniteTransition = rememberInfiniteTransition(label = "banner")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = ComposeRepeatMode.Reverse
        ), label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AppColors.GoldAlpha20)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(AppColors.GoldAlpha10, AppColors.CardDark, AppColors.CardDark)
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(AppColors.Gold)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PROCHAIN APPEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Gold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        call.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%02d:%02d".format(call.scheduledHour, call.scheduledMinute),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (hours > 0) "dans ${hours}h ${minutes}min" else "dans ${minutes}min",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// STATISTIQUES
// ─────────────────────────────────────────────

@Composable
fun StatsPanel(uiState: UiState) {
    val successRate = if (uiState.history.isNotEmpty()) {
        val completed = uiState.history.count { it.status == CallStatus.COMPLETED }
        (completed * 100f / uiState.history.size).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardDark),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("TABLEAU DE BORD", style = MaterialTheme.typography.labelSmall, color = AppColors.Gold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("${uiState.calls.size}", "Total", AppColors.CyanAccent)
                StatItem("${uiState.enabledCount}", "Actifs", AppColors.GreenSuccess)
                StatItem("${uiState.totalCallsMade}", "Passés", AppColors.Gold)
                StatItem("$successRate%", "Succès", AppColors.PurpleAccent)
            }
            Spacer(Modifier.height(16.dp))
            Text("Taux de réussite", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.Divider)
            ) {
                val progressWidth = (successRate / 100f).coerceAtLeast(0.02f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressWidth)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AppColors.GreenSuccess, AppColors.Gold))
                        )
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp), color = color, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
    }
}

// ─────────────────────────────────────────────
// LISTE DES APPELS
// ─────────────────────────────────────────────

@Composable
fun CallList(
    calls: List<ScheduledCall>,
    onToggle: (ScheduledCall) -> Unit,
    onDelete: (ScheduledCall) -> Unit,
    onEdit: (ScheduledCall) -> Unit,
    onDuplicate: (ScheduledCall) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = calls.groupBy { it.scheduledHour }

        grouped.forEach { (hour, hourCalls) ->
            item(key = "header_$hour") {
                TimelineHeader(hour, hourCalls.size)
            }
            items(hourCalls, key = { it.id }) { call ->
                CallCard(
                    call = call,
                    onToggle = { onToggle(call) },
                    onDelete = { onDelete(call) },
                    onEdit = { onEdit(call) },
                    onDuplicate = { onDuplicate(call) }
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TimelineHeader(hour: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%02dh".format(hour),
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.Gold,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(AppColors.GoldAlpha20, Color.Transparent))
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$count appel${if (count > 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextMuted
        )
    }
}

// ─────────────────────────────────────────────
// CARTE D'APPEL
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallCard(
    call: ScheduledCall,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val accentColor = when {
        !call.isEnabled -> AppColors.StatusInactive
        call.lastCallStatus == CallStatus.COMPLETED -> AppColors.GreenSuccess
        call.lastCallStatus == CallStatus.BUSY -> AppColors.OrangeWarning
        call.lastCallStatus == CallStatus.FAILED -> AppColors.RedError
        else -> AppColors.Gold
    }

    val cardAlpha = if (call.isEnabled) 1f else 0.55f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(cardAlpha)
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.CardDark, AppColors.CardDarkAlt),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    if (call.isEnabled) AppColors.Divider else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (call.groupTag.isNotEmpty()) {
                        Text(
                            call.groupTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.CyanAccent,
                            modifier = Modifier
                                .background(AppColors.CyanAlpha30, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        call.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        call.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    if (call.dtmfExtension.isNotEmpty()) {
                        Text(
                            "  poste ${call.dtmfExtension}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChipInfo(
                        icon = Icons.Default.Schedule,
                        text = "%02d:%02d".format(call.scheduledHour, call.scheduledMinute),
                        color = AppColors.Gold
                    )
                    ChipInfo(
                        icon = Icons.Default.Repeat,
                        text = call.repeatMode.shortLabel,
                        color = AppColors.CyanAccent
                    )
                    if (call.simSlot >= 0) {
                        ChipInfo(
                            icon = Icons.Default.SimCard,
                            text = "SIM ${call.simSlot + 1}",
                            color = AppColors.PurpleAccent
                        )
                    }
                    if (call.totalCallsMade > 0) {
                        ChipInfo(
                            icon = Icons.Default.CheckCircle,
                            text = "${call.totalCallsMade}×",
                            color = AppColors.GreenSuccess
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(AppColors.CardDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier", color = AppColors.TextPrimary) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = AppColors.Gold, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Dupliquer", color = AppColors.TextPrimary) },
                            onClick = { showMenu = false; onDuplicate() },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = AppColors.CyanAccent, modifier = Modifier.size(18.dp)) }
                        )
                        Divider(color = AppColors.Divider)
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = AppColors.RedError) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = AppColors.RedError, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Switch(
                    checked = call.isEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.TextOnGold,
                        checkedTrackColor = AppColors.Gold,
                        uncheckedThumbColor = AppColors.TextMuted,
                        uncheckedTrackColor = AppColors.Divider
                    )
                )
            }
        }
    }
}

@Composable
fun ChipInfo(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

val RepeatMode.shortLabel get() = when(this) {
    RepeatMode.ONCE -> "1×"
    RepeatMode.DAILY -> "∞ Quotidien"
    RepeatMode.WEEKLY -> "∞ Hebdo"
    RepeatMode.WEEKDAYS -> "∞ Lun-Ven"
    RepeatMode.WEEKENDS -> "∞ Week-end"
    RepeatMode.CUSTOM_DAYS -> "∞ Personnalisé"
    RepeatMode.INTERVAL -> "∞ Intervalle"
    else -> "∞"
}

// ─────────────────────────────────────────────
// FAB PULSANT
// ─────────────────────────────────────────────

@Composable
fun PulseFab(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = ComposeRepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(AppColors.GoldAlpha10)
        )
        FloatingActionButton(
            onClick = onClick,
            containerColor = AppColors.Gold,
            contentColor = AppColors.TextOnGold,
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter un appel", modifier = Modifier.size(28.dp))
        }
    }
}

// ─────────────────────────────────────────────
// ÉTATS VIDE / CHARGEMENT
// ─────────────────────────────────────────────

@Composable
fun EmptyScreen(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📞", fontSize = 64.sp)
        Spacer(Modifier.height(20.dp))
        Text("Aucun appel planifié", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Appuyez sur + pour commencer", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.TextOnGold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Planifier un appel", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.Gold)
    }
}
