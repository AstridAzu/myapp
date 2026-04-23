package com.example.myapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

data class AppTopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val tint: Color = Color.Unspecified
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    showHomeAction: Boolean = true,
    showNotificationsAction: Boolean = true,
    onHomeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    actions: List<AppTopBarAction> = emptyList(),
    containerColor: Color = Color(0xFF0D1117),
    titleColor: Color = Color.White,
    actionIconTint: Color = Color.White
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            navigationIcon?.invoke()
        },
        actions = {
            if (showHomeAction) {
                IconButton(onClick = onHomeClick) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = actionIconTint
                    )
                }
            }

            if (showNotificationsAction) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = actionIconTint
                    )
                }
            }

            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        tint = if (action.tint == Color.Unspecified) actionIconTint else action.tint
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleColor,
            actionIconContentColor = actionIconTint,
            navigationIconContentColor = actionIconTint
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}