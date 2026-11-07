package com.example.appstore.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.appstore.data.AppStatus
import com.example.appstore.data.StoreApp

/** Бегущий блик для скелетонов и загрузки картинок */
@Composable
fun Modifier.shimmer(): Modifier {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        0f, 1200f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x"
    )
    return background(
        Brush.linearGradient(
            listOf(cs.surfaceContainerHigh, cs.surfaceContainerHighest, cs.surfaceContainerHigh),
            start = Offset(x - 400f, 0f), end = Offset(x, 0f)
        )
    )
}

/** Логотип: блик пока грузится, значок Android если картинки нет, тонкая рамка */
@Composable
fun AppIcon(url: String, size: Dp, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.24f)
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.5f), shape),
        loading = { Box(Modifier.fillMaxSize().shimmer()) },
        error = {
            Box(
                Modifier.fillMaxSize().background(cs.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Android, null,
                    tint = cs.onSecondaryContainer, modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    )
}

/** Логотип с кольцом прогресса вокруг (страница приложения) */
@Composable
fun DownloadingIcon(url: String, size: Dp, progress: Float?) {
    val anim by animateFloatAsState(progress ?: 0f, label = "progress")
    val scale by animateFloatAsState(if (progress != null) 0.86f else 1f, label = "scale")
    Box(Modifier.size(size + 20.dp), contentAlignment = Alignment.Center) {
        AppIcon(url, size, Modifier.scale(scale))
        if (progress != null) {
            if (progress <= 0f) {
                CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
            } else {
                CircularProgressIndicator(
                    progress = { anim }, modifier = Modifier.fillMaxSize(), strokeWidth = 4.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/** Квадратная карточка: логотип + название, бейдж статуса и кольцо загрузки */
@Composable
fun SquareCard(
    app: StoreApp,
    status: AppStatus = AppStatus.NotInstalled,
    progress: Float? = null,
    onClick: () -> Unit
) {
    Box(Modifier.size(132.dp)) {
        ElevatedCard(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppIcon(app.icon, 68.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    app.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        when (status) {
            is AppStatus.Installed -> Icon(
                Icons.Rounded.CheckCircle, "Установлено",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            )
            is AppStatus.UpdateAvailable -> Surface(
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Text(
                    "Обновить", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            AppStatus.NotInstalled -> Unit
        }
        if (progress != null) {
            val anim by animateFloatAsState(progress, label = "p")
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    if (progress <= 0f) {
                        CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
                    } else {
                        CircularProgressIndicator(
                            progress = { anim }, modifier = Modifier.fillMaxSize(), strokeWidth = 4.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round
                        )
                        Text("${(anim * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/** Широкая карточка в стиле Steam-капсулы для блока "Рекомендуем" */
@Composable
fun FeaturedCard(app: StoreApp, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .width(320.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer)))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            AppIcon(app.icon, 96.dp)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = cs.onPrimaryContainer
                )
                Text(
                    app.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = cs.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Скелетон ленты вместо крутилки */
@Composable
fun FeedSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            Modifier.padding(horizontal = 16.dp).width(320.dp).height(160.dp)
                .clip(RoundedCornerShape(28.dp)).shimmer()
        )
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.padding(horizontal = 16.dp).width(140.dp).height(24.dp)
                        .clip(RoundedCornerShape(8.dp)).shimmer()
                )
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { Box(Modifier.size(132.dp).clip(RoundedCornerShape(24.dp)).shimmer()) }
                }
            }
        }
    }
}
