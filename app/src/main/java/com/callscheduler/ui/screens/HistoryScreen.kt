package com.callscheduler.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.callscheduler.data.model.CallHistoryEntry
import com.callscheduler.data.model.CallStatus
import com.callscheduler.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<CallHistoryEntry>,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppColors.SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historique", style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                        Text("${history.size} appels enregistrés", style = MaterialTheme.typography.bodySmall, color = AppColors.Gold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = AppColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.SpaceBlack)
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Aucun historique", color = AppColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Grouper par date
                val grouped = history.groupBy {
                    SimpleDateFormat("d MMMM yyyy", Locale.FRENCH).format(Date(it.timestamp))
                }
                grouped.forEach { (date, entries) ->
                    item(key = "date_$date") {
                        Text(
                            date.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Gold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(entries, key = { it.id }) { entry ->
                        HistoryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(entry: CallHistoryEntry) {
    val (color, icon, label) = when (entry.status) {
        CallStatus.COMPLETED -> Triple(AppColors.GreenSuccess, Icons.Default.CheckCircle, "Réussi")
        CallStatus.BUSY -> Triple(AppColors.OrangeWarning, Icons.Default.PhoneMissed, "Occupé")
        CallStatus.FAILED -> Triple(AppColors.RedError, Icons.Default.Cancel, "Échoué")
        CallStatus.CANCELLED -> Triple(AppColors.TextMuted, Icons.Default.RemoveCircle, "Annulé")
        else -> Triple(AppColors.CyanAccent, Icons.Default.Phone, entry.status.name)
    }

    val time = SimpleDateFormat("HH:mm:ss", Locale.FRENCH).format(Date(entry.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(entry.phoneNumber, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (entry.attemptNumber > 1) {
                    Text("Tentative #${entry.attemptNumber}", style = MaterialTheme.typography.labelSmall, color = AppColors.OrangeWarning)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                if (entry.durationSeconds > 0) {
                    Text(
                        "${entry.durationSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted
                    )
                }
            }
        }
    }
}
