package com.example.docvault.ui.vault

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.Description
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.docvault.R
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory
import com.example.docvault.ui.components.LoadingScreen
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch

/**
 * The main dashboard of DocVault.
 * 
 * Features premium Productivity & Finance UI with curved headers and bold typography.
 * Supports Categorized saving with a metadata confirmation dialog and a "Saved" popup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val qrResult by viewModel.qrResult.collectAsState(initial = null)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showSourcePicker by remember { mutableStateOf(false) }
    var isQrScanningMode by remember { mutableStateOf(false) }
    var showCombineDialog by remember { mutableStateOf(false) }
    
    // Metadata Dialog State for saving new docs
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileType by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val selectedDocIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode by remember { derivedStateOf { selectedDocIds.isNotEmpty() } }

    LaunchedEffect(qrResult) {
        qrResult?.let { content ->
            if (content.startsWith("http")) uriHandler.openUri(content)
            else Toast.makeText(context, "QR Content: $content", Toast.LENGTH_LONG).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            if (isQrScanningMode) {
                try {
                    val image = InputImage.fromFilePath(context, it)
                    viewModel.scanQrCode(image)
                } catch (e: Exception) {
                    Toast.makeText(context, "QR Scan Failed", Toast.LENGTH_SHORT).show()
                }
                isQrScanningMode = false
            } else {
                pendingUri = it
                pendingFileType = context.contentResolver.getType(it) ?: "image/jpeg"
                showSaveDialog = true
            }
        }
    }

    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pdf?.let { pdf ->
                pendingUri = pdf.uri
                pendingFileType = "application/pdf"
                showSaveDialog = true
            } ?: scanningResult?.pages?.firstOrNull()?.let { page ->
                pendingUri = page.imageUri
                pendingFileType = "image/jpeg"
                showSaveDialog = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VaultHeader(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedDocIds.size,
                onClearSelection = { selectedDocIds.clear() },
                onHistoryClick = onNavigateToHistory,
                onCombineClick = { showCombineDialog = true },
                onDeleteSelection = {
                    selectedDocIds.forEach { id ->
                        uiState.documents.find { it.id == id }?.let { viewModel.deleteDocument(it) }
                    }
                    selectedDocIds.clear()
                    scope.launch {
                        snackbarHostState.showSnackbar("Deleted selected documents")
                    }
                },
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = { viewModel.onCategorySelect(it) },
                totalDocs = uiState.documents.size
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !isSelectionMode, enter = fadeIn(), exit = fadeOut()) {
                FloatingActionButton(
                    onClick = { showSourcePicker = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingScreen(message = "Accessing encrypted vault...")
        } else {
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                if (uiState.documents.isEmpty() && uiState.searchQuery.isEmpty()) {
                    EmptyVaultContent()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.documents, key = { it.id }) { document ->
                            val isSelected = selectedDocIds.contains(document.id)
                            DocumentCard(
                                document = document, 
                                isSelected = isSelected,
                                onClick = { 
                                    if (isSelectionMode) {
                                        if (isSelected) selectedDocIds.remove(document.id)
                                        else selectedDocIds.add(document.id)
                                    } else onNavigateToDetail(document.id) 
                                },
                                onLongClick = { if (!isSelected) selectedDocIds.add(document.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showSourcePicker) {
            ModalBottomSheet(
                onDismissRequest = { showSourcePicker = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                SourcePickerContent(
                    onGalleryClick = { isQrScanningMode = false; showSourcePicker = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onScannerClick = { 
                        showSourcePicker = false
                        (context as? android.app.Activity)?.let { scanner.getStartScanIntent(it).addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                        }}
                    },
                    onQrClick = { isQrScanningMode = true; showSourcePicker = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
            }
        }

        if (showSaveDialog && pendingUri != null) {
            var docTitle by remember { mutableStateOf("") }
            var docCat by remember { mutableStateOf(DocumentCategory.OTHER) }
            var catExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Document Info", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it },
                            label = { Text("Title") },
                            placeholder = { Text("e.g. My ID Card") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box {
                            OutlinedTextField(
                                value = docCat.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { IconButton(onClick = { catExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                            )
                            DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                DocumentCategory.entries.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.name) }, onClick = { docCat = cat; catExpanded = false })
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (docTitle.isNotBlank()) {
                                context.contentResolver.openInputStream(pendingUri!!)?.let { stream ->
                                    viewModel.addDocument(docTitle, docCat, pendingFileType!!, stream, "imported_file")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Document saved to vault")
                                    }
                                }
                                showSaveDialog = false
                            }
                        },
                        enabled = docTitle.isNotBlank()
                    ) { Text("Save to Vault") }
                },
                dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
            )
        }

        if (showCombineDialog) {
            var pdfTitle by remember { mutableStateOf("Combined Doc") }
            AlertDialog(
                onDismissRequest = { showCombineDialog = false },
                title = { Text("Bundle images into PDF", fontWeight = FontWeight.Bold) },
                text = { OutlinedTextField(value = pdfTitle, onValueChange = { pdfTitle = it }, label = { Text("Document Title") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) },
                confirmButton = { Button(onClick = { 
                    viewModel.combineToPdf(selectedDocIds.toList(), pdfTitle)
                    selectedDocIds.clear()
                    showCombineDialog = false 
                    scope.launch {
                        snackbarHostState.showSnackbar("Combined PDF saved to vault")
                    }
                }) { Text("Create PDF") } },
                dismissButton = { TextButton(onClick = { showCombineDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

/**
 * Custom Header inspired by high-end dashboards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHeader(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onHistoryClick: () -> Unit,
    onCombineClick: () -> Unit,
    onDeleteSelection: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: DocumentCategory?,
    onCategorySelect: (DocumentCategory?) -> Unit,
    totalDocs: Int
) {
    val isDark = isSystemInDarkTheme()
    
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(if (isSelectionMode) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(bottom = if (isSelectionMode) 0.dp else 24.dp)) {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSelectionMode) Text("$selectedCount Selected", style = MaterialTheme.typography.titleMedium)
                        else Text("DOCVAULT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = onClearSelection) { Icon(Icons.Default.Close, contentDescription = null) }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.evault),
                                contentDescription = "DocVault Logo",
                                modifier = Modifier.padding(start = 16.dp).size(40.dp).clip(CircleShape)
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = onCombineClick) { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                            IconButton(onClick = onDeleteSelection) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        } else {
                            IconButton(onClick = onHistoryClick) { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = Color.White) }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                )
                
                if (!isSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Secure Vault.",
                                style = MaterialTheme.typography.headlineLarge.copy(color = Color.White)
                            )
                            Text(
                                text = "High-performance encrypted storage.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                            )
                        }
                        
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$totalDocs FILES",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
        
        if (!isSelectionMode) {
            Spacer(Modifier.height(16.dp))
            CategoryFilters(selectedCategory, onCategorySelect)
            SearchBar(searchQuery, onSearchQueryChange)
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        placeholder = { Text("Search by title or tags...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun CategoryFilters(selectedCategory: DocumentCategory?, onCategorySelect: (DocumentCategory?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryTab(label = "All", isSelected = selectedCategory == null, onClick = { onCategorySelect(null) }, modifier = Modifier.weight(1f))
        DocumentCategory.entries.take(3).forEach { category ->
            CategoryTab(
                label = category.name.lowercase().replaceFirstChar { it.uppercase() },
                isSelected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CategoryTab(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCard(document: Document, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (document.fileType == "application/pdf") Icons.AutoMirrored.Filled.Note else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.align(Alignment.End).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.weight(1f))
            
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${document.size / 1024} KB • ${document.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun EmptyVaultContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Vault is Empty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your files are encrypted and safe here.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun SourcePickerContent(
    onGalleryClick: () -> Unit,
    onScannerClick: () -> Unit,
    onQrClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Add to Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SourceCard("Gallery", Icons.Default.PhotoLibrary, onGalleryClick, Modifier.weight(1f))
            SourceCard("Scanner", Icons.Default.DocumentScanner, onScannerClick, Modifier.weight(1f))
            SourceCard("QR Code", Icons.Default.QrCodeScanner, onQrClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun SourceCard(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
