package com.example.myapp.ui.rutinas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.theme.CategoryCardColor

// ─── helpers ──────────────────────────────────────────────────────────────────
// El color e ícono vienen de la DB (colorHex / icono en RutinaEntity).
// colorHexToColor e iconoKeyToFuente proveen fallbacks si son null.
// ─── screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    navController: NavController,
    viewModel: RutinasViewModel
) {
    val presetRutinas by viewModel.presetRutinas.collectAsState()
    val misRutinas    by viewModel.misRutinas.collectAsState()
    val isSyncing     by viewModel.isSyncing.collectAsState()
    val syncError     by viewModel.syncError.collectAsState()
    val lastSyncAt    by viewModel.lastSyncAt.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Rutinas",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                onHomeClick = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Main.route)
                        launchSingleTop = true
                    }
                },
                onNotificationsClick = {
                    navController.navigate(Routes.Notificaciones.route) {
                        launchSingleTop = true
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.CrearRutina.createRoute(viewModel.idUsuario)) },
                icon    = { Icon(Icons.Default.Add, contentDescription = null) },
                text    = { Text("Nueva Rutina") },
                containerColor = CategoryCardColor,
                contentColor   = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sync Status Bar
            SyncStatusBar(
                isSyncing = isSyncing,
                syncError = syncError,
                lastSyncAt = lastSyncAt,
                onRefreshClick = { viewModel.syncNow() }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
            // ── Sección: Rutinas Predefinidas ──────────────────────────────
            item {
                SectionHeader(title = "Rutinas predefinidas")
            }

            if (presetRutinas.isEmpty()) {
                item {
                    Text(
                        text = "Cargando rutinas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(presetRutinas) { rutina ->
                    PresetRutinaCard(
                        rutina  = rutina,
                        onClick = { navController.navigate(Routes.RutinaDetalle.createRoute(rutina.id, viewModel.idUsuario)) }
                    )
                }
            }

            // ── Sección: Mis Rutinas ───────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Mis rutinas")
            }

            if (misRutinas.isEmpty()) {
                item {
                    EmptyRutinasHint()
                }
            } else {
                items(misRutinas, key = { it.id }) { rutina ->
                    RutinaCard(
                        rutina    = rutina,
                        onClick   = { navController.navigate(Routes.RutinaDetalle.createRoute(rutina.id, viewModel.idUsuario)) },
                        onDelete  = { viewModel.eliminarRutina(rutina.id) }
                    )
                }
            }

            // Espacio extra para que el FAB no tape el último item
            item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── components ───────────────────────────────────────────────────────────────

@Composable
private fun SyncStatusBar(
    isSyncing: Boolean,
    syncError: String?,
    lastSyncAt: Long,
    onRefreshClick: () -> Unit
) {
    if (isSyncing || syncError != null || lastSyncAt > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = when {
                        syncError != null -> MaterialTheme.colorScheme.errorContainer
                        isSyncing -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isSyncing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sincronizando...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    syncError != null -> {
                        Text(
                            text = "Error de sync: $syncError",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    lastSyncAt > 0 -> {
                        Text(
                            text = "Última sincronización: ${formatLastSyncTime(lastSyncAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            if (!isSyncing) {
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sincronizar ahora",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatLastSyncTime(timeMs: Long): String {
    val diff = System.currentTimeMillis() - timeMs
    return when {
        diff < 60_000 -> "hace unos segundos"
        diff < 3_600_000 -> "hace ${diff / 60_000} min"
        diff < 86_400_000 -> "hace ${diff / 3_600_000} h"
        else -> "hace ${diff / 86_400_000} días"
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text  = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/** Tarjeta para las rutinas predefinidas del sistema: borde lateral coloreado + ícono. */
@Composable
private fun PresetRutinaCard(rutina: RutinaEntity, onClick: () -> Unit) {
    val accentColor = colorHexToColor(rutina.colorHex)
    val icono       = iconoKeyToFuente(rutina.icono)

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Franja lateral de color
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .background(accentColor)
            )
            // Contenido
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text  = rutina.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                rutina.descripcion?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text     = it,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Ícono del tipo de rutina
            IconoIcon(
                fuente             = icono,
                contentDescription = null,
                tint               = accentColor.copy(alpha = 0.8f),
                modifier           = Modifier
                    .padding(end = 16.dp)
                    .size(36.dp)
            )
        }
    }
}

/** Tarjeta para las rutinas del usuario. */
@Composable
private fun RutinaCard(rutina: RutinaEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val accentColor = colorHexToColor(rutina.colorHex)
    val icono       = iconoKeyToFuente(rutina.icono)
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Eliminar rutina") },
            text  = { Text("¿Eliminar \"${rutina.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showConfirm = false },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text       = rutina.nombre,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                rutina.descripcion?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text     = it,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (rutina.activa) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text       = "ACTIVA",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor,
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconoIcon(
                fuente             = icono,
                contentDescription = null,
                tint               = accentColor.copy(alpha = 0.8f),
                modifier           = Modifier.size(32.dp)
            )
            IconButton(onClick = { showConfirm = true }) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Eliminar rutina",
                    tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** Estado vacío en "Mis rutinas". */
@Composable
private fun EmptyRutinasHint() {
    Box(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment  = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier           = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Todavía no tienes rutinas propias.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text  = "Usa el botón + para crear una.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

