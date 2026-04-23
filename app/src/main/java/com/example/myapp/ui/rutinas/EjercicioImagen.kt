package com.example.myapp.ui.rutinas

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun EjercicioImagen(
    imageUrl: String?,
    localImageUri: String? = null,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val localModel = localImageUri?.trim()?.takeIf { it.isNotEmpty() }?.let(Uri::parse)
    val remoteModel = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
    val model = localModel ?: remoteModel
    if (model == null) {
        EjercicioImagenFallback(modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Fit,
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        },
        error = {
            EjercicioImagenFallback(modifier = Modifier.fillMaxSize())
        },
        success = {
            SubcomposeAsyncImageContent()
        }
    )
}

@Composable
private fun EjercicioImagenFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}
