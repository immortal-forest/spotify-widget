package xyz.immortalforest.widgets.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.palette.graphics.Palette
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.immortalforest.widgets.widget.models.WImage
import xyz.immortalforest.widgets.widget.presentation.Loading
import xyz.immortalforest.widgets.widget.util.SpotifyHelper
import xyz.immortalforest.widgets.widget.util.imageLoaderr
import xyz.immortalforest.widgets.widget.util.requestAsyncImage


class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = SmallWidget()
}


class SmallWidget : GlanceAppWidget() {

    // spotify stuff
    private val spotifyHelper = SpotifyHelper()
    private var spotifyAppRemote: SpotifyAppRemote? = null

    private val loading = mutableStateOf(false)
    private val image = mutableStateOf(WImage())
    private val trackUri = mutableStateOf("")
    private val playerRestrictions = mutableStateOf(PlayerRestrictions.DEFAULT)
    private val paused = mutableStateOf(true)

    private val defaultContainerColor = (0xFF888888).toInt()
    private val defaultIconColor = (0xFF000000).toInt()

    private val containerColor = mutableStateOf(Color(defaultContainerColor).copy(alpha = 0.5f))
    private val iconColor = mutableStateOf(Color(defaultIconColor))

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        spotifyHelper.new(context, id)
        connectToSpotify()

        provideContent {
            Column(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Under redesign",
                    style = TextStyle(
                        color = ColorProvider(Color.Black),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                )
            }

        }

    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        spotifyAppRemote = null
        spotifyHelper.disconnect()
        super.onDelete(context, glanceId)
    }

    private fun updateUI(context: Context, id: GlanceId) {
        CoroutineScope(Dispatchers.IO).launch {
            update(context, id)
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
        if (loading.value.not()) {
            loading.value = true
        }
        spotifyAppRemote!!.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
            if (paused.value != playerState.isPaused) {
                paused.value = playerState.isPaused
            }
            if (playerRestrictions.value != playerState.playbackRestrictions) {
                playerRestrictions.value = playerState.playbackRestrictions
            }
            if (trackUri.value != playerState.track.uri) {
                trackUri.value = playerState.track.uri
                if (image.value.uri != playerState.track.imageUri) {
                    image.value = WImage(playerState.track.imageUri, image.value.bitmap)
                    CoroutineScope(Dispatchers.IO).launch {
                        loadImage(context, id, playerState.track.imageUri)
                    }
                }
            }
            updateUI(context, id)

        }.setErrorCallback {
            paused.value = true
            updateUI(context, id)
            connectToSpotify()
        }
    }

    private fun loadImage(context: Context, id: GlanceId, imageUri: ImageUri) {
        val imageLoader = context.imageLoaderr
        imageLoader.requestAsyncImage(context, imageUri) { drawable ->
            val bitMap = Bitmap.createScaledBitmap(
                (drawable as BitmapDrawable).bitmap,
                (150 * 2.6).toInt(), (150 * 2.6).toInt(),
                true
            )
            CoroutineScope(Dispatchers.IO).launch {
                Palette.from(bitMap).generate().let { palette ->
                    containerColor.value =
                        (palette.lightVibrantSwatch?.rgb?.let { Color(it) }
                            ?: Color(defaultContainerColor)).copy(
                            alpha = 0.5f
                        )
                    iconColor.value =
                        (palette.darkVibrantSwatch?.titleTextColor?.let { Color(it) }
                            ?: Color(defaultIconColor))
                }
                updateUI(context, id)
            }
            image.value = WImage(
                imageUri,
                bitMap
            )
            updateUI(context, id)
        }
    }

}
