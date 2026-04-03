package com.example.ringtones

import android.Manifest
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

data class Ringtone(
    val name: String,
    val assetFileName: String
)

class RingtoneViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaPlayer: MediaPlayer? = null
    private var points = 0

    private val _playingRingtone = MutableStateFlow<String?>(null)
    val playingRingtone: StateFlow<String?> = _playingRingtone

    private val _points = MutableStateFlow(0)
    val pointsFlow: StateFlow<Int> = _points

    private val _lastShared = MutableStateFlow<String?>(null)
    val lastShared: StateFlow<String?> = _lastShared

    val ringtones = listOf(
        Ringtone("Dragon Ball", "dragon-ball.mp3"),
        Ringtone("Mario Bros Jump", "mario-bros-jump.mp3"),
        Ringtone("Pacman Dies", "pacman-dies.mp3"),
        Ringtone("Grillos", "ringtones-grillos.mp3"),
        Ringtone("Kill Bill Whistle", "ringtones-kill-bill-whistle.mp3"),
        Ringtone("iPhone Original", "ringtones-original-iphone.mp3"),
        Ringtone("Pink Panther", "ringtones-pink-panther.mp3"),
        Ringtone("Super Mario Bros", "ringtones-super-mario-bros.mp3"),
        Ringtone("UEFA Champions League", "uefa-champions-league-theme.mp3"),
        Ringtone("Zelda Navi", "zelda-navi-listen.mp3")
    )

    fun playOrPause(context: Context, ringtone: Ringtone) {
        if (_playingRingtone.value == ringtone.assetFileName) {
            mediaPlayer?.pause()
            _playingRingtone.value = null
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val afd = context.assets.openFd(ringtone.assetFileName)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                start()
                setOnCompletionListener {
                    _playingRingtone.value = null
                }
            }
            _playingRingtone.value = ringtone.assetFileName
        }
    }

    fun download(context: Context, ringtone: Ringtone) {
        try {
            val inputStream = context.assets.open(ringtone.assetFileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, ringtone.assetFileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        inputStream.copyTo(output)
                    }
                }
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC
                )
                if (!musicDir.exists()) musicDir.mkdirs()
                val outputFile = File(musicDir, ringtone.assetFileName)
                FileOutputStream(outputFile).use { output ->
                    inputStream.copyTo(output)
                }
            }
            Toast.makeText(context, "${ringtone.name} guardado en Música", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun share(context: Context, ringtone: Ringtone) {
        try {
            val cacheFile = File(context.cacheDir, ringtone.assetFileName)
            context.assets.open(ringtone.assetFileName).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            points++
            _points.value = points
            _lastShared.value = ringtone.name
            showPointsNotification(context, ringtone.name)

            context.startActivity(Intent.createChooser(intent, "Compartir ${ringtone.name} vía..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPointsNotification(context: Context, ringtoneName: String) {
        val notification = NotificationCompat.Builder(context, "ringtone_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Ganaste un punto!")
            .setContentText("Compartiste \"$ringtoneName\" — Total: $points ${if (points == 1) "punto" else "puntos"}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(points, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(points, notification)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}