package xyz.immortalforest.widgets.widget

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.immortalforest.widgets.widget.models.WImage
import xyz.immortalforest.widgets.widget.models.WTrack
import xyz.immortalforest.widgets.widget.presentation.Loading
import xyz.immortalforest.widgets.widget.presentation.MediumContent

class MediumWidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = MediumWidget()

}


class MediumWidget: GlanceAppWidget() {

    // spotify
    private val spotifyHelper = SpotifyHelper()
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // data
    private val loading = mutableStateOf(false)
    private val paused = mutableStateOf(false)
    private val image = mutableStateOf(WImage())
    private val playerRestrictions = mutableStateOf(PlayerRestrictions.DEFAULT)
    private val track = mutableStateOf(WTrack(""))


//    override val sizeMode: SizeMode
//        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        spotifyHelper.new(context, id)
        connectToSpotify()

        provideContent {
            if (!loading.value && spotifyAppRemote == null) {
                Loading()
            } else {
                TODO("MediumWidgetContent()")
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        spotifyAppRemote = null
        spotifyHelper.disconnect()
        super.onDelete(context, glanceId)
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
            if (image.value.uri != playerState.track.imageUri) {
                spotifyAppRemote!!.imagesApi.getImage(playerState.track.imageUri).setResultCallback { bitmap ->
                    image.value = WImage(playerState.track.imageUri, bitmap)
                }
            }
            if (playerRestrictions.value != playerState.playbackRestrictions) {
                playerRestrictions.value = playerState.playbackRestrictions
            }
            if (track.value.uri != playerState.track.uri) {
                track.value = WTrack(playerState.track)
            }
            updateUI(context, id)
        }
    }


    private fun updateUI(context: Context, glanceId: GlanceId) {
        CoroutineScope(Dispatchers.IO).launch {
            update(context, glanceId)
        }
    }

}
