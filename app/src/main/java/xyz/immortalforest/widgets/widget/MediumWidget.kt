package xyz.immortalforest.widgets.widget

import android.content.Context
import android.content.Intent
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

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
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
                        loadImage(image.value.uri)
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
        spotifyHelper.connectToSpotify(
            onConnect = { context, id, appRemote ->
                spotifyAppRemote = appRemote
                updateUI(context, id)
                listenToUpdates(context, id)
            },
            onError = { context, id ->
                paused.value = true
                updateUI(context, id)
            },
        )

    }

    private fun listenToUpdates(context: Context, id: GlanceId) {
        if (!loading.value) {
            loading.value = true
        }
        spotifyAppRemote!!.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
            paused.value = playerState.isPaused
            if (track.value.uri != playerState.track.uri) {
                track.value = WTrack().createTrack(playerState.track)
                updateUI(context, id)
            }
            if (image.value.uri != playerState.track.imageUri) {
                spotifyAppRemote!!.imagesApi.getImage(playerState.track.imageUri)
                    .setResultCallback { bitmap ->
                        image.value = WImage(playerState.track.imageUri, bitmap)
                        CoroutineScope(Dispatchers.IO).launch {
                            Palette.from(bitmap).generate().let { palette ->
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
            }
            playerRestrictions.value = playerState.playbackRestrictions
            updateUI(context, id)
        }
    }

    private fun loadImage(imageUri: ImageUri) {
        spotifyAppRemote!!.imagesApi.getImage(imageUri).setResultCallback {
            image.value = WImage(imageUri, it)
        }
    }

}
