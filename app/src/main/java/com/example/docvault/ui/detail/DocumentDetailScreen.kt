package com.example.docvault.ui.detail

import android.content.Intent
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
//mport androidx.compose.material.icons.automirrored.filled.Description
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory
import com.example.docvault.ui.components.LoadingScreen
import kotlinx.coroutines.launch

/**
 * Screen displaying the details of a document.
 * 
 * Redesigned with premium Productivity UI.
 * Features a high-contrast preview and categorized metadata view.
 * Supports Feature 4 (Edit & Delete) and Sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DocumentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProcessSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = { 
                        val uri = viewModel.getShareUri(context)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = context.contentResolver.getType(uri) ?: "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Document"))
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Failed to prepare file for sharing") }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
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
                        Text("Optimize Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingScreen(message = "Fetching encrypted data...")
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
            title = { Text("Permanently Delete?") },
            text = { Text("This will remove the encrypted file from your local storage. This action cannot be undone.") },
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
            onConfirm = { title, category -> 
                viewModel.updateMetadata(title, category)
                showEditDialog = false
                scope.launch { snackbarHostState.showSnackbar("Document updated") }
            }
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
                    scope.launch { snackbarHostState.showSnackbar("Processing started...") }
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
            text = "Added on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(document.createdAt))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Preview Card with high corners
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
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
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
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
        
        // Enhanced Metadata Sections
        DetailSection(
            title = "File Properties",
            items = listOf(
                "Category" to document.category.name,
                "Format" to document.fileType.substringAfter("/").uppercase(),
                "Size" to "${document.size / 1024} KB"
            )
        )
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun DetailSection(title: String, items: List<Pair<String, String>>) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            items.forEachIndexed { index, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pair.first, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(pair.second, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                if (index < items.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
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
        title = { Text("Update Document", fontWeight = FontWeight.Bold) },
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
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onConfirm(title, category) }) { Text("Save") } },
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

    Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Optimize & Transform", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Manage storage impact effectively.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(24.dp))
        
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Column {
                ListItem(
                    headlineContent = { Text("Balanced Compression", fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Default.HighQuality, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Switch(checked = compressChecked, onCheckedChange = { compressChecked = it; if (it) aggressiveChecked = false }) },
                    modifier = Modifier.clickable { compressChecked = !compressChecked; if (compressChecked) aggressiveChecked = false },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Aggressive (< 40KB)", fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Default.Speed, null, tint = Color(0xFFE67E22)) },
                    trailingContent = { Switch(checked = aggressiveChecked, onCheckedChange = { aggressiveChecked = it; if (it) compressChecked = false }) },
                    modifier = Modifier.clickable { aggressiveChecked = !aggressiveChecked; if (aggressiveChecked) compressChecked = false },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (!isPdf) {
                    ListItem(
                        headlineContent = { Text("Generate PDF Format", fontWeight = FontWeight.Bold) },
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
            Text("Process Now", fontWeight = FontWeight.ExtraBold)
        }
    }
}
