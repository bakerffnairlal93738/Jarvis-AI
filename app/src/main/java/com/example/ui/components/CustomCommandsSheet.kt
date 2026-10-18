package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomCommandEntity
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCommandsSheet(
    commands: List<CustomCommandEntity>,
    onAddCommand: (String, String, String) -> Unit,
    onDeleteCommand: (CustomCommandEntity) -> Unit,
    onTestCommand: (CustomCommandEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var triggerText by remember { mutableStateOf("") }
    var targetUrlText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JarvisDarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CUSTOM VOICE COMMANDS",
                        color = JarvisCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Add voice triggers mapped to actions or URLs",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { isAddingNew = !isAddingNew },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAddingNew) JarvisCardBg else JarvisCyan,
                        contentColor = if (isAddingNew) JarvisCyan else JarvisObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_add_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Command",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAddingNew) "Close" else "New", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isAddingNew) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisCardBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "NEW VOICE COMMAND",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = triggerText,
                            onValueChange = { triggerText = it },
                            label = { Text("Spoken Trigger Phrase (e.g. 'open my website')") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisCardBorder,
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("command_trigger_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = targetUrlText,
                            onValueChange = { targetUrlText = it },
                            label = { Text("Target URL (e.g. 'https://github.com')") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisCardBorder,
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("command_url_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            label = { Text("Optional Description") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisCardBorder,
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (triggerText.isNotBlank() && targetUrlText.isNotBlank()) {
                                    onAddCommand(triggerText.trim(), targetUrlText.trim(), descriptionText.trim())
                                    triggerText = ""
                                    targetUrlText = ""
                                    descriptionText = ""
                                    isAddingNew = false
                                }
                            },
                            enabled = triggerText.isNotBlank() && targetUrlText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("save_command_button")
                        ) {
                            Text("SAVE COMMAND", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (commands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom commands configured yet.",
                        color = JarvisTextDim,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commands) { cmd ->
                        CommandCard(
                            command = cmd,
                            onTest = { onTestCommand(cmd) },
                            onDelete = { onDeleteCommand(cmd) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CommandCard(
    command: CustomCommandEntity,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisCardBg)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "\"${command.triggerPhrase}\"",
                color = JarvisCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = command.targetUrl,
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
            if (command.description.isNotBlank()) {
                Text(
                    text = command.description,
                    color = JarvisTextDim,
                    fontSize = 11.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onTest,
                modifier = Modifier.size(36.dp).testTag("test_command_${command.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Test Command",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp).testTag("delete_command_${command.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Command",
                    tint = JarvisCrimson,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
