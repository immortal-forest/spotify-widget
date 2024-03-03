package xyz.immortalforest.widgets.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.immortalforest.widgets.widget.models.WImage
import xyz.immortalforest.widgets.widget.presentation.Loading
import xyz.immortalforest.widgets.widget.presentation.SmallContent


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


    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        spotifyHelper.new(context, id)
        connectToSpotify()

        provideContent {
            if (!loading.value && spotifyAppRemote == null) {
                Loading()
            } else {
                SmallContent(
                    spotifyHelper,
                    loading.value,
                    playerRestrictions.value,
                    image.value.bitmap,
                    paused.value,
                    {
                        ContextCompat.startActivity(
                            context,
                            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:track:${trackUri.value}"))
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

    private fun updateUI(context: Context, id: GlanceId) {
        CoroutineScope(Dispatchers.IO).launch {
            update(context, id)
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
        if (loading.value.not()) {
            loading.value = true
        }
        spotifyAppRemote!!.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
            if (paused.value != playerState.isPaused) {
                paused.value = playerState.isPaused
            }
            if (image.value.uri != playerState.track.imageUri) {
                spotifyAppRemote!!.imagesApi.getImage(playerState.track.imageUri)
                    .setResultCallback { bitmap ->
                        image.value = WImage(
                            playerState.track.imageUri,
                            bitmap
                        )
                    }
            }
            if (playerRestrictions.value != playerState.playbackRestrictions) {
                playerRestrictions.value = playerState.playbackRestrictions
            }
            if (trackUri.value != playerState.track.uri) {
                trackUri.value = playerState.track.uri
            }
            updateUI(context, id)

        }.setErrorCallback {
            paused.value = true
            updateUI(context, id)
        }
    }

    private fun loadImage(imageUri: ImageUri) {
        spotifyAppRemote!!.imagesApi.getImage(imageUri).setResultCallback {
            image.value = WImage(imageUri, it)
        }
    }

}

