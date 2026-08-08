package com.example.docvault.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory
import com.example.docvault.ui.components.LoadingScreen

/**
 * Screen displaying the details of a document.
 * 
 * Inspired by modern smart-app designs with clean typography, 
 * large rounded corners, and deep surface colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DocumentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProcessSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (uiState.document != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = { showProcessSheet = true },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Optimize & Convert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingScreen(message = "Fetching Document...")
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            uiState.document?.let { document ->
                DocumentDetailContent(
                    document = document,
                    modifier = Modifier.padding(padding),
                    getDecryptedStream = { viewModel.getDecryptedStream(it) }
                )
            }
        }
    }

    // Dialogs & Sheets
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Document?") },
            text = { Text("This will permanently remove the encrypted file from your device storage.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteDocument(); showDeleteDialog = false; onNavigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditDialog && uiState.document != null) {
        EditDocumentDialog(
            document = uiState.document!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, category -> viewModel.updateMetadata(title, category); showEditDialog = false }
        )
    }

    if (showProcessSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProcessSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            ProcessSheetContent(
                currentSize = uiState.document?.size ?: 0L,
                isPdf = uiState.document?.fileType == "application/pdf",
                onConfirm = { compress, toPdf, aggressive ->
                    viewModel.processDocument(compress || aggressive, toPdf, targetSizeKb = if (aggressive) 39 else null)
                    showProcessSheet = false
                }
            )
        }
    }
}

@Composable
fun DocumentDetailContent(
    document: Document,
    modifier: Modifier = Modifier,
    getDecryptedStream: (String) -> java.io.InputStream
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = document.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Last updated ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(document.updatedAt))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (document.fileType.startsWith("image/")) {
                    val bitmap = remember(document.filePath, document.updatedAt) {
                        try { getDecryptedStream(document.filePath).use { android.graphics.BitmapFactory.decodeStream(it) } } 
                        catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, modifier = Modifier.size(80.dp), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("PDF Document", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Metadata Sections
        SectionHeader("Technical Details")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                InfoRow("Status", "Encrypted", isStatus = true)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                InfoRow("Category", document.category.name)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                InfoRow("Format", document.fileType.substringAfter("/").uppercase())
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                InfoRow("Size", "${document.size / 1024} KB")
            }
        }
        
        Spacer(Modifier.height(100.dp)) // Extra space for bottom bar
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String, isStatus: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isStatus) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(8.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EditDocumentDialog(
    document: Document,
    onDismiss: () -> Unit,
    onConfirm: (String, DocumentCategory) -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    var category by remember { mutableStateOf(document.category) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Edit Metadata", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = category.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DocumentCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onConfirm(title, category) }) { Text("Save Changes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProcessSheetContent(
    currentSize: Long,
    isPdf: Boolean,
    onConfirm: (compress: Boolean, toPdf: Boolean, aggressive: Boolean) -> Unit
) {
    var compressChecked by remember { mutableStateOf(false) }
    var toPdfChecked by remember { mutableStateOf(false) }
    var aggressiveChecked by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 16.dp)) {
        Text("Optimize Document", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Impact: ${currentSize / 1024} KB", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(24.dp))
        
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Column {
                ListItem(
                    headlineContent = { Text("Standard Compression", fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Default.HighQuality, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Switch(checked = compressChecked, onCheckedChange = { compressChecked = it; if (it) aggressiveChecked = false }) },
                    modifier = Modifier.clickable { compressChecked = !compressChecked; if (compressChecked) aggressiveChecked = false },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Compress under 40KB", fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Default.Speed, null, tint = Color(0xFFE67E22)) },
                    trailingContent = { Switch(checked = aggressiveChecked, onCheckedChange = { aggressiveChecked = it; if (it) compressChecked = false }) },
                    modifier = Modifier.clickable { aggressiveChecked = !aggressiveChecked; if (aggressiveChecked) compressChecked = false },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (!isPdf) {
                    ListItem(
                        headlineContent = { Text("Generate PDF", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.secondary) },
                        trailingContent = { Switch(checked = toPdfChecked, onCheckedChange = { toPdfChecked = it }) },
                        modifier = Modifier.clickable { toPdfChecked = !toPdfChecked },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onConfirm(compressChecked, toPdfChecked, aggressiveChecked) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = compressChecked || toPdfChecked || aggressiveChecked
        ) {
            Text("Apply Selected Actions", fontWeight = FontWeight.ExtraBold)
        }
    }
}
