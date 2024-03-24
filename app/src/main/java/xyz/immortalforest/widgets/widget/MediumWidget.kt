package xyz.immortalforest.widgets.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.request.ImageRequest
import coil.request.Parameters
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.immortalforest.widgets.widget.models.WImage
import xyz.immortalforest.widgets.widget.models.WTrack
import xyz.immortalforest.widgets.widget.presentation.Loading
import xyz.immortalforest.widgets.widget.presentation.MediumContent
import xyz.immortalforest.widgets.widget.util.SpotifyHelper
import java.io.File

class MediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = MediumWidget()

}


class MediumWidget : GlanceAppWidget() {

    // spotify stuff
    private val spotifyHelper = SpotifyHelper()
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // data
    private val loading = mutableStateOf(false)
    private val paused = mutableStateOf(false)
    private val image = mutableStateOf(WImage())
    private val playerRestrictions = mutableStateOf(PlayerRestrictions.DEFAULT)
    private val track = mutableStateOf(WTrack())
    private val defaultContainerColor = (0xFF888888).toInt()

    private val containerColor = mutableStateOf(Color(defaultContainerColor).copy(alpha = 0.5f))
    private val containerTextColor = mutableStateOf(Color.Black)
    private val iconColor = mutableStateOf(Color.Black)

    private lateinit var imageLoader: ImageLoader

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        imageLoader = ImageLoader.Builder(context.applicationContext)
            .diskCache {
                val cacheDir = File(context.externalCacheDir?.absolutePath.toString(), "album")
                cacheDir.mkdirs()
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(1024 * 1024 * 69)
                    .build()
            }
            .build()
        spotifyHelper.new(context, id)
        connectToSpotify()

        provideContent {
            if (!loading.value && spotifyAppRemote == null) {
                Loading()
            } else {
                MediumContent(
                    spotifyHelper = spotifyHelper,
                    loading = loading.value,
                    playerRestrictions = playerRestrictions.value,
                    imageBitmap = image.value.bitmap,
                    isPaused = paused.value,
                    containerColor = containerColor.value,
                    containerTextColor = containerTextColor.value,
                    iconColor = iconColor.value,
                    track = track.value,
                    {
                        ContextCompat.startActivity(
                            context,
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("spotify:track:${track.value.uri}")
                            )
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            Bundle.EMPTY
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            loadImage(context, id, track.value.imageUri)
                        }
                    },
                    { context, id ->
                        updateUI(context, id)
                    }
                ) {
                    connectToSpotify()
                }
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        spotifyAppRemote = null
        spotifyHelper.disconnect()
        super.onDelete(context, glanceId)
    }

    private fun updateUI(context: Context, glanceId: GlanceId) {
        CoroutineScope(Dispatchers.IO).launch {
            update(context, glanceId)
        }
    }

    private fun connectToSpotify() {
        spotifyHelper.disconnect()
        spotifyHelper.connectToSpotify(
            onConnect = { context, id, appRemote ->
                spotifyAppRemote = appRemote
                updateUI(context, id)
                CoroutineScope(Dispatchers.IO).launch {
                    listenToUpdates(context, id)
                }
            },
            onError = { context, id ->
                paused.value = true
                updateUI(context, id)
                connectToSpotify()
            },
        )

    }

    private fun listenToUpdates(context: Context, id: GlanceId) {
        if (!loading.value) {
            loading.value = true
        }
        spotifyAppRemote!!.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
            if (paused.value != playerState.isPaused) {
                paused.value = playerState.isPaused
            }
            if (track.value.uri != playerState.track.uri) {
                track.value = WTrack(playerState.track)
                if (image.value.uri != playerState.track.imageUri) {
                    image.value = WImage(playerState.track.imageUri, image.value.bitmap)
                    CoroutineScope(Dispatchers.IO).launch {
                        loadImage(context, id, playerState.track.imageUri)
                    }
                }
            }
            if (playerRestrictions.value != playerState.playbackRestrictions) {
                playerRestrictions.value = playerState.playbackRestrictions
            }
            updateUI(context, id)
        }.setErrorCallback {
            paused.value = true
            updateUI(context, id)
            connectToSpotify()
        }
    }

    private fun loadImage(context: Context, id: GlanceId, imageUri: ImageUri) {
        val uri = Regex("spotify:image:(.*)").find(imageUri.raw.toString())?.groups?.get(1)?.value
            ?: imageUri.raw.toString().replace("spotify:image:", "")

        val request = ImageRequest.Builder(context)
            .data("https://i.scdn.co/image/$uri")
            .parameters(
                Parameters.Builder()
                    .set("Content-Type", "image/jpeg")
                    .build()
            )
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .decoderFactory(
                Decoder.Factory { result, options, _ ->
                    return@Factory BitmapFactoryDecoder(result.source, options)
                }
            )
            .target { drawable ->
                val bitMap = Bitmap.createScaledBitmap(
                    (drawable as BitmapDrawable).bitmap,
                    (92 * 2.69).toInt(), (95 * 2.69).toInt(),
                    true
                )
                image.value = WImage(
                    imageUri,
                    bitMap
                )
                CoroutineScope(Dispatchers.IO).launch {
                    Palette.from(bitMap).generate().let { palette ->
                        containerColor.value =
                            (palette.lightVibrantSwatch?.rgb?.let { Color(it) }
                                ?: Color(defaultContainerColor)).copy(
                                alpha = 0.8f
                            )
                        containerTextColor.value =
                            (palette.lightVibrantSwatch?.titleTextColor?.let { Color(it) }
                                ?: Color.Black)
                        iconColor.value =
                            (palette.darkVibrantSwatch?.rgb?.let { Color(it) }
                                ?: Color.Black)
                    }
                    updateUI(context, id)
                }
            }
            .build()
        imageLoader.enqueue(request)
    }

}
