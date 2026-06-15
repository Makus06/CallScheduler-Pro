package com.callscheduler.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.foundation.text.KeyboardOptions
import com.callscheduler.data.model.*
import com.callscheduler.ui.theme.AppColors

// ─────────────────────────────────────────────
// ÉCRAN AJOUT / MODIFICATION D'APPEL
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCallScreen(
    existingCall: ScheduledCall? = null,
    onSave: (ScheduledCall) -> Unit,
    onCancel: () -> Unit,
) {
    val isEditing = existingCall != null

    // États du formulaire
    var label by remember { mutableStateOf(existingCall?.label ?: "") }
    var phoneNumber by remember { mutableStateOf(existingCall?.phoneNumber ?: "") }
    var dtmfExtension by remember { mutableStateOf(existingCall?.dtmfExtension ?: "") }
    var groupTag by remember { mutableStateOf(existingCall?.groupTag ?: "") }
    var notes by remember { mutableStateOf(existingCall?.notes ?: "") }

    var selectedHour by remember { mutableStateOf(existingCall?.scheduledHour ?: 8) }
    var selectedMinute by remember { mutableStateOf(existingCall?.scheduledMinute ?: 0) }
    var repeatMode by remember { mutableStateOf(existingCall?.repeatMode ?: RepeatMode.DAILY) }
    var simSlot by remember { mutableStateOf(existingCall?.simSlot ?: 0) }

    var autoRedial by remember { mutableStateOf(existingCall?.autoRedialOnBusy ?: true) }
    var redialMax by remember { mutableStateOf(existingCall?.autoRedialMaxAttempts ?: 3) }
    var redialDelay by remember { mutableStateOf(existingCall?.autoRedialDelaySeconds ?: 60) }

    var isEnabled by remember { mutableStateOf(existingCall?.isEnabled ?: true) }

    var showTimePicker by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    // Validation
    fun validate(): Boolean {
        return when {
            label.isBlank() -> { formError = "Le nom est obligatoire"; false }
            phoneNumber.isBlank() -> { formError = "Le numéro est obligatoire"; false }
            phoneNumber.replace(Regex("[^0-9+]"), "").length < 7 -> { formError = "Numéro invalide"; false }
            else -> { formError = ""; true }
        }
    }

    fun buildCall(): ScheduledCall {
        val base = existingCall ?: ScheduledCall(
            label = label,
            phoneNumber = phoneNumber,
            scheduledHour = selectedHour,
            scheduledMinute = selectedMinute,
            startDate = System.currentTimeMillis()
        )
        return base.copy(
            label = label.trim(),
            phoneNumber = phoneNumber.trim(),
            dtmfExtension = dtmfExtension.trim(),
            groupTag = groupTag.trim(),
            notes = notes.trim(),
            scheduledHour = selectedHour,
            scheduledMinute = selectedMinute,
            repeatMode = repeatMode,
            simSlot = simSlot,
            autoRedialOnBusy = autoRedial,
            autoRedialMaxAttempts = redialMax,
            autoRedialDelaySeconds = redialDelay,
            isEnabled = isEnabled,
        )
    }

    Scaffold(
        containerColor = AppColors.SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Modifier l'appel" else "Nouvel appel",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, null, tint = AppColors.TextSecondary)
                    }
                },
                actions = {
                    // Bouton Enregistrer
                    Button(
                        onClick = { if (validate()) onSave(buildCall()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Gold,
                            contentColor = AppColors.TextOnGold
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enregistrer", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.SpaceBlack
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Erreur
            if (formError.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.RedError.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = AppColors.RedError, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(formError, color = AppColors.RedError, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ── IDENTITÉ ─────────────────────────────────

            FormSection("IDENTITÉ", Icons.Default.Label) {
                AppTextField(label = "Nom de l'appel *", value = label, onValueChange = { label = it },
                    placeholder = "Ex: Razik Fin", leadingIcon = Icons.Default.DriveFileRenameOutline)
                AppTextField(label = "Étiquette / Groupe", value = groupTag, onValueChange = { groupTag = it },
                    placeholder = "Ex: Razik", leadingIcon = Icons.Default.Group)
                AppTextField(label = "Notes", value = notes, onValueChange = { notes = it },
                    placeholder = "Optionnel", leadingIcon = Icons.Default.Notes, singleLine = false)
            }

            // ── NUMÉRO ────────────────────────────────────

            FormSection("NUMÉRO DE TÉLÉPHONE", Icons.Default.Phone) {
                AppTextField(
                    label = "Numéro *", value = phoneNumber, onValueChange = { phoneNumber = it },
                    placeholder = "+33...", leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
                AppTextField(
                    label = "Poste (DTMF)", value = dtmfExtension, onValueChange = { dtmfExtension = it },
                    placeholder = "Ex: 739286", leadingIcon = Icons.Default.Dialpad,
                    keyboardType = KeyboardType.Number
                )
                Text(
                    "Le poste sera composé automatiquement en DTMF après la connexion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted
                )
            }

            // ── HORAIRE ───────────────────────────────────

            FormSection("HORAIRE", Icons.Default.Schedule) {
                // Sélecteur d'heure visuel
                TimeSelector(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourChanged = { selectedHour = it },
                    onMinuteChanged = { selectedMinute = it }
                )

                Spacer(Modifier.height(8.dp))
                Text("MODE DE RÉPÉTITION", style = MaterialTheme.typography.labelSmall, color = AppColors.Gold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))

                // Grille de sélection du mode
                val modes = RepeatMode.values().toList()
                val rows = modes.chunked(3)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { mode ->
                            RepeatModeChip(
                                mode = mode,
                                selected = repeatMode == mode,
                                onClick = { repeatMode = mode },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Remplir les espaces vides si la ligne est incomplète
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── SIM ───────────────────────────────────────

            FormSection("CARTE SIM", Icons.Default.SimCard) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Pair(0, "SIM 1"), Pair(1, "SIM 2"), Pair(-1, "Auto")).forEach { (slot, lbl) ->
                        FilterChip(
                            selected = simSlot == slot,
                            onClick = { simSlot = slot },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.GoldAlpha20,
                                selectedLabelColor = AppColors.Gold,
                                containerColor = AppColors.CardDark,
                                labelColor = AppColors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selectedBorderColor = AppColors.Gold,
                                borderColor = AppColors.Divider,
                                borderWidth = 1.dp, selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }
            }

            // ── RECOMPOSITION ─────────────────────────────

            FormSection("RECOMPOSITION AUTOMATIQUE", Icons.Default.Refresh) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Si occupé, recomposer", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                        Text("Relance automatique en cas d'occupation", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    }
                    Switch(
                        checked = autoRedial,
                        onCheckedChange = { autoRedial = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.TextOnGold,
                            checkedTrackColor = AppColors.Gold,
                        )
                    )
                }

                AnimatedVisibility(visible = autoRedial) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        // Tentatives max
                        StepperRow(
                            label = "Tentatives max",
                            value = redialMax,
                            min = 1, max = 10,
                            onDecrease = { if (redialMax > 1) redialMax-- },
                            onIncrease = { if (redialMax < 10) redialMax++ }
                        )
                        // Délai entre tentatives
                        StepperRow(
                            label = "Délai entre tentatives",
                            value = redialDelay,
                            unit = "s",
                            min = 10, max = 300, step = 10,
                            onDecrease = { if (redialDelay > 10) redialDelay -= 10 },
                            onIncrease = { if (redialDelay < 300) redialDelay += 10 }
                        )
                    }
                }
            }

            // ── ACTIVATION ────────────────────────────────

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) AppColors.GreenAlpha20 else AppColors.CardDark
                ),
                border = BorderStroke(1.dp, if (isEnabled) AppColors.GreenSuccess.copy(alpha = 0.3f) else AppColors.Divider)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isEnabled) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                            null,
                            tint = if (isEnabled) AppColors.GreenSuccess else AppColors.TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (isEnabled) "Appel activé" else "Appel désactivé",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isEnabled) AppColors.GreenSuccess else AppColors.TextMuted
                            )
                            Text(
                                if (isEnabled) "Cet appel sera planifié automatiquement" else "Cet appel ne sera pas exécuté",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.TextOnGold,
                            checkedTrackColor = AppColors.GreenSuccess,
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────
// COMPOSANTS FORMULAIRE
// ─────────────────────────────────────────────

@Composable
fun FormSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardDark),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = AppColors.Gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = { Text(placeholder, color = AppColors.TextMuted) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 4,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Gold,
            unfocusedBorderColor = AppColors.Divider,
            focusedLabelColor = AppColors.Gold,
            unfocusedLabelColor = AppColors.TextMuted,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            cursorColor = AppColors.Gold,
            focusedLeadingIconColor = AppColors.Gold,
            unfocusedLeadingIconColor = AppColors.TextMuted,
        ),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = leadingIcon?.let { { Icon(it, null, modifier = Modifier.size(20.dp)) } }
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
fun TimeSelector(hour: Int, minute: Int, onHourChanged: (Int) -> Unit, onMinuteChanged: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.SpaceBlack),
        border = BorderStroke(1.dp, AppColors.GoldAlpha20)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heure
            NumberPicker(value = hour, min = 0, max = 23, onChanged = onHourChanged, label = "H")
            Text(":", style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp), color = AppColors.Gold, modifier = Modifier.padding(horizontal = 8.dp))
            // Minute
            NumberPicker(value = minute, min = 0, max = 59, step = 1, onChanged = onMinuteChanged, label = "MIN")
        }
    }
}

@Composable
fun NumberPicker(value: Int, min: Int, max: Int, step: Int = 1, onChanged: (Int) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        IconButton(onClick = { if (value < max) onChanged(value + step) else onChanged(min) }) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = AppColors.Gold, modifier = Modifier.size(28.dp))
        }
        Text(
            "%02d".format(value),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Black
        )
        IconButton(onClick = { if (value > min) onChanged(value - step) else onChanged(max) }) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = AppColors.Gold, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun RepeatModeChip(mode: RepeatMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) AppColors.GoldAlpha20 else AppColors.SpaceBlack
    val border = if (selected) AppColors.Gold else AppColors.Divider
    val textColor = if (selected) AppColors.Gold else AppColors.TextSecondary

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            mode.shortLabel.replace("∞ ", ""),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun StepperRow(
    label: String,
    value: Int,
    unit: String = "",
    min: Int = 1,
    max: Int = 100,
    step: Int = 1,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier
                    .size(34.dp)
                    .background(AppColors.Divider, CircleShape)
            ) {
                Icon(Icons.Default.Remove, null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
            }
            Text(
                "$value$unit",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Gold,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier
                    .size(34.dp)
                    .background(AppColors.GoldAlpha20, CircleShape)
            ) {
                Icon(Icons.Default.Add, null, tint = AppColors.Gold, modifier = Modifier.size(16.dp))
            }
        }
    }
}
