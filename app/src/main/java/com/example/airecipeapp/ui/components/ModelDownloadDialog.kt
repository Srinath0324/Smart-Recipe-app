package com.example.airecipeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airecipeapp.domain.ml.DownloadProgress
import com.example.airecipeapp.domain.ml.RequirementsCheck

@Composable
fun ModelDownloadDialog(
    isModelDownloaded: Boolean,
    requirementsCheck: RequirementsCheck,
    downloadProgress: DownloadProgress,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("AI Model")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Model info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SmolLM-135M-Instruct",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Size: 101 MB",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Offline AI recipe generation",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Requirements check
                if (!isModelDownloaded) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (requirementsCheck.hasEnoughStorage && requirementsCheck.hasEnoughRam) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Requirements",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            RequirementRow(
                                label = "Storage",
                                required = "${requirementsCheck.requiredStorageMB} MB",
                                available = "${requirementsCheck.availableStorageMB} MB",
                                isMet = requirementsCheck.hasEnoughStorage
                            )
                            
                            RequirementRow(
                                label = "RAM",
                                required = "${requirementsCheck.requiredRamMB} MB",
                                available = "${requirementsCheck.availableRamMB} MB",
                                isMet = requirementsCheck.hasEnoughRam
                            )
                        }
                    }
                }
                
                // Download progress
                when (downloadProgress) {
                    is DownloadProgress.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { downloadProgress.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Downloading: ${(downloadProgress.progress * 100).toInt()}% " +
                                        "(${downloadProgress.downloadedBytes / (1024 * 1024)} / " +
                                        "${downloadProgress.totalBytes / (1024 * 1024)} MB)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is DownloadProgress.Verifying -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text(
                                text = "Verifying model integrity...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is DownloadProgress.Complete -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Model ready!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is DownloadProgress.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = downloadProgress.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (isModelDownloaded) {
                TextButton(onClick = onDeleteClick) {
                    Text("Delete Model")
                }
            } else {
                Button(
                    onClick = onDownloadClick,
                    enabled = requirementsCheck.hasEnoughStorage && 
                             requirementsCheck.hasEnoughRam &&
                             downloadProgress !is DownloadProgress.Downloading &&
                             downloadProgress !is DownloadProgress.Verifying
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RequirementRow(
    label: String,
    required: String,
    available: String,
    isMet: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "$available / $required",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
