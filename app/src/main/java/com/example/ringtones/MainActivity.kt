package com.example.ringtones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BuenFinRed = Color(0xFFCC0000)
val BuenFinYellow = Color(0xFFFFD700)
val BuenFinDark = Color(0xFF1A1A2E)
val CardBg = Color(0xFF16213E)

class MainActivity : ComponentActivity() {

    private val viewModel: RingtoneViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        createNotificationChannel()

        setContent {
            RingtoneGalleryApp(viewModel)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ringtone_channel",
                "Ringtone Points",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de puntos por compartir ringtones"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun RingtoneGalleryApp(viewModel: RingtoneViewModel) {
    val playingRingtone by viewModel.playingRingtone.collectAsState()
    val points by viewModel.pointsFlow.collectAsState()
    val lastShared by viewModel.lastShared.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lastShared) {
        if (lastShared != null) {
            snackbarHostState.showSnackbar(
                message = "¡+1 punto! Compartiste \"$lastShared\" — Total: $points puntos",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(listOf(BuenFinDark, CardBg))
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🛍️ El Buen Fin",
                    color = BuenFinYellow,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Galería de Ringtones",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "⭐ Puntos: $points",
                    color = BuenFinYellow,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(viewModel.ringtones) { ringtone ->
                        RingtoneCard(
                            ringtone = ringtone,
                            isPlaying = playingRingtone == ringtone.assetFileName,
                            onPlayPause = { viewModel.playOrPause(context, ringtone) },
                            onDownload = { viewModel.download(context, ringtone) },
                            onShare = { viewModel.share(context, ringtone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RingtoneCard(
    ringtone: Ringtone,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎵 ${ringtone.name}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (isPlaying) "Reproduciendo..." else "Listo para reproducir",
                    color = if (isPlaying) BuenFinYellow else Color.Gray,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .background(
                        color = if (isPlaying) BuenFinYellow else BuenFinRed,
                        shape = RoundedCornerShape(50)
                    )
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .background(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(50)
                    )
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Descargar",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .background(
                        color = Color(0xFF1565C0),
                        shape = RoundedCornerShape(50)
                    )
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = Color.White
                )
            }
        }
    }
}