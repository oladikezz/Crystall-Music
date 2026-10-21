package com.crystall.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.*

private val DEFAULT_TOP_ARTISTS = listOf(
    "Miyagi & Эндшпиль",
    "The Weeknd",
    "MACAN",
    "Billie Eilish",
    "Travis Scott",
    "Скриптонит",
    "Drake",
    "Anna Asti",
    "Linkin Park",
    "Eminem",
    "ATL",
    "PHARAOH",
    "Big Baby Tape",
    "Taylor Swift",
    "Kendrick Lamar",
    "Imagine Dragons",
    "Arctic Monkeys",
    "Jony",
    "SALUKI",
    "Markul",
    "КИНО",
    "Король и Шут"
)

/**
 * YouTube Music Clean Light Taste Picker Sheet
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TastePickerSheet(
    initialSelectedArtists: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val strings = LocalStrings.current
    val selectedArtists = remember { mutableStateListOf<String>().apply { addAll(initialSelectedArtists) } }
    var customArtistInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val allArtists = remember {
        val list = mutableStateListOf<String>()
        list.addAll(DEFAULT_TOP_ARTISTS)
        for (artist in initialSelectedArtists) {
            if (!list.contains(artist)) {
                list.add(0, artist)
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    spotColor = Color(0x30000000)
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(LightSurface)
                .border(
                    width = 1.dp,
                    color = LightBorder,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1D5DB))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LightSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = LightTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = strings.tasteTitle,
                            color = LightTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = LightTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.tasteSubtitle,
                    color = LightTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Artist Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightSurfaceElevated)
                        .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = customArtistInput,
                        onValueChange = { customArtistInput = it },
                        placeholder = {
                            Text(
                                text = "Добавить своего исполнителя...",
                                color = LightTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = LightTextPrimary,
                            unfocusedTextColor = LightTextPrimary,
                            cursorColor = LightTextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val trimmed = customArtistInput.trim()
                                if (trimmed.isNotBlank()) {
                                    if (!allArtists.contains(trimmed)) allArtists.add(0, trimmed)
                                    if (!selectedArtists.contains(trimmed)) selectedArtists.add(trimmed)
                                    customArtistInput = ""
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (customArtistInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val trimmed = customArtistInput.trim()
                                if (trimmed.isNotBlank()) {
                                    if (!allArtists.contains(trimmed)) allArtists.add(0, trimmed)
                                    if (!selectedArtists.contains(trimmed)) selectedArtists.add(trimmed)
                                    customArtistInput = ""
                                }
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = LightTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Flow of Artist Chips
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            allArtists.forEach { artist ->
                                val isSelected = selectedArtists.contains(artist)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isSelected) LightChipSelectedBackground else LightChipBackground)
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isSelected) LightChipSelectedBackground else LightBorder,
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .clickable {
                                            if (isSelected) {
                                                selectedArtists.remove(artist)
                                            } else {
                                                selectedArtists.add(artist)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = artist,
                                            color = if (isSelected) LightChipSelectedText else LightTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Action Bar (Skip & Done)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(LightSurfaceElevated)
                            .border(1.dp, LightBorder, RoundedCornerShape(24.dp))
                            .clickable { onSkip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.tasteSkipBtn,
                            color = LightTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Apply / Save Button
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(LightTextPrimary)
                            .clickable {
                                onSave(selectedArtists.toList())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedArtists.isEmpty()) strings.tasteSaveBtn else "${strings.tasteSaveBtn} (${selectedArtists.size})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
