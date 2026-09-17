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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.ui.theme.TextPrimary
import com.crystall.music.ui.theme.TextSecondary

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TastePickerSheet(
    initialSelectedArtists: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onSave: (List<String>) -> Unit
) {
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
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xF01A1B29),
                            Color(0xF811121C),
                            Color(0xFF090A10)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x4DFFFFFF), Color(0x10FFFFFF))
                    ),
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

                // Dynamic Island Drag Handle
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x4DFFFFFF))
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
                                .background(Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Музыкальный вкус",
                            color = TextPrimary,
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
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Выберите любимых артистов для персонализации вашей ленты",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Artist Input (Liquid Glass Capsule)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = customArtistInput,
                        onValueChange = { customArtistInput = it },
                        placeholder = {
                            Text(
                                text = "Добавить своего исполнителя...",
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                                tint = Color.White,
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
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0x38FFFFFF) else Color(0x12FFFFFF),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "chip_bg"
                                )
                                val borderColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else Color(0x22FFFFFF),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "chip_border"
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(bgColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(20.dp))
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
                                            color = if (isSelected) Color.White else Color(0xD8FFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
                            .background(Color(0x16FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                            .clickable { onSkip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Пропустить",
                            color = Color(0xB3FFFFFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Apply / Save Button
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.White.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .clickable {
                                onSave(selectedArtists.toList())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedArtists.isEmpty()) "Применить" else "Готово (${selectedArtists.size})",
                            color = Color(0xFF101014),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
