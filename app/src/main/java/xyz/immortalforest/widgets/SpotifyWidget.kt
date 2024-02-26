package xyz.immortalforest.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SpotifyWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SpotifyWidget
}

private object SpotifyWidget : GlanceAppWidget() {

    var spotifyAppRemote: SpotifyAppRemote? = null
    val loadingState = mutableStateOf(true)
    private val playerRestrictions = mutableStateOf<PlayerRestrictions>(PlayerRestrictions.DEFAULT)
    private val imageUri = mutableStateOf(ImageUri(""))
    private val imageBitmapState =
        mutableStateOf(Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888))
    private val pauseState = mutableStateOf(false)

    private val smallSize = DpSize(193.66667.dp, 169.33333.dp)
//    private val mediumSize = DpSize(258.22266.dp, 169.33333.dp)
//    private val largeSize = DpSize()

    override val sizeMode: SizeMode = SizeMode.Responsive(listOf(smallSize).toSet())


    override suspend fun provideGlance(context: Context, id: GlanceId) {
        connectToSpotify(context, id)

        provideContent {
            GlanceTheme {
                if (loadingState.value.not() && spotifyAppRemote == null) {
                    Loading()
                } else {
                    ResponsiveContent(context = context, id = id, size = LocalSize.current)
                }
            }
        }
    }

    fun connectToSpotify(context: Context, id: GlanceId) {
        val clientId = "371ac845c4264fed92fc27a5224a4426"
        val redirectUri = "https://localhost:6969"

        // Set the connection parameters
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                // to remove loading
                CoroutineScope(Dispatchers.IO).launch {
                    update(context, id)
                }
                spotifyAppRemote!!.playerApi.subscribeToPlayerState().setEventCallback {
                    if (imageUri.value != it.track.imageUri) {
                        imageUri.value = it.track.imageUri
                        spotifyAppRemote!!.imagesApi.getImage(it.track.imageUri)
                            .setResultCallback { bitmap ->
                                imageBitmapState.value = bitmap
                            }
                            .setErrorCallback { throwable ->
                                imageBitmapState.value =
                                    Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
                                Log.e("SpotifyAppRemote", "An error occurred!", throwable)
                            }
                    }
                    if (loadingState.value.not()) {
                        loadingState.value = true
                    }

                    if (playerRestrictions.value != it.playbackRestrictions) {
                        playerRestrictions.value = it.playbackRestrictions
                    }
                    if (pauseState.value != it.isPaused) {
                        pauseState.value = it.isPaused
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        update(context, id)
                    }
                }
            }

            override fun onFailure(throwable: Throwable) {
                // Something went wrong when attempting to connect! Handle errors here
                Log.e("SpotifyAppRemote", "Error", throwable)
                pauseState.value = true
                CoroutineScope(Dispatchers.IO).launch {
                    update(context, id)
                }
            }
        })


    }

    private fun reloadImage(context: Context, id: GlanceId, imageUri: ImageUri) {
        spotifyAppRemote?.imagesApi?.getImage(imageUri)?.setResultCallback {
            imageBitmapState.value = it
        }
        CoroutineScope(Dispatchers.IO).launch {
            update(context, id)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        super.onDelete(context, glanceId)
    }

    @Composable
    private fun Loading() {
        Column(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = ColorProvider(Color.DarkGray)
            )
        }
    }

    @Composable
    private fun ResponsiveContent(
        context: Context,
        id: GlanceId,
        size: DpSize,
    ) {
        if (LocalSize.current == size) {
            SmallContent(
                playerRestrictions.value,
                imageBitmapState.value,
                {
                    ContextCompat.startActivity(
                        context, Intent(Intent.ACTION_VIEW, Uri.parse("spotify:app"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        Bundle.EMPTY
                    )
                    reloadImage(context, id, imageUri.value)
                },
                pauseState.value
            )
        }  else {
            Loading()
        }
    }

    @Composable
    private fun SmallContent(
        playerRestrictions: PlayerRestrictions,
        imageBitmap: Bitmap,
        imageClick: () -> Unit,
        isPaused: Boolean,
    ) {

        Box {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Box(
                    modifier = GlanceModifier.size(150.dp)
                        .cornerRadius(12.dp)
                ) {
                    Image(
                        provider = ImageProvider(imageBitmap),
                        contentDescription = "Album art",
                        modifier = GlanceModifier.fillMaxSize()
                            .clickable(imageClick),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = GlanceModifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.spotify),
                    contentDescription = "Spotify icon",
//                    contentScale = ContentScale.FillBounds
                )
            }
        }
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = GlanceModifier
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(
                            alpha = 0.5f
                        )
                    ).size(130.dp, 50.dp).cornerRadius(12.dp),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize()
                        .padding(start = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val key = ActionParameters.Key<String>("action")
                    val value = ActionParameters.Key<String>("value")
                    Image(
                        modifier = GlanceModifier.size(40.dp).clickable(
                            actionRunCallback(
                                ButtonAction::class.java, actionParametersOf(
                                    key to "prev",
                                    value to playerRestrictions.canSkipPrev.toString()
                                )
                            )
                        )
                            .cornerRadius(16.dp),
                        provider = ImageProvider(R.drawable.prev_foreground),
                        contentDescription = "Previous",
                    )
                    Image(
                        modifier = GlanceModifier.size(40.dp).clickable(
                            actionRunCallback(
                                ButtonAction::class.java, actionParametersOf(
                                    key to "play_pause", value to if (isPaused) {
                                        "play"
                                    } else {
                                        "pause"
                                    }
                                )
                            )
                        )
                            .cornerRadius(16.dp),
                        provider = ImageProvider(
                            if (isPaused) {
                                R.drawable.play_foreground
                            } else {
                                R.drawable.pause_foreground
                            }
                        ),
                        contentDescription = "Play"
                    )
                    Image(
                        modifier = GlanceModifier.size(40.dp).clickable(
                            actionRunCallback(
                                ButtonAction::class.java, actionParametersOf(
                                    key to "next",
                                    value to playerRestrictions.canSkipNext.toString()
                                )
                            )
                        )
                            .cornerRadius(16.dp),
                        provider = ImageProvider(R.drawable.next_foreground),
                        contentDescription = "Next"
                    )
                }

            }

        }
    }
}

class ButtonAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (SpotifyWidget.spotifyAppRemote == null || SpotifyWidget.loadingState.value) {
            // re-connect
            CoroutineScope(Dispatchers.Main).launch {
                SpotifyWidget.connectToSpotify(context, glanceId)
            }
        }
        when (parameters[ActionParameters.Key("action")].toString()) {
            "prev" ->
                if (parameters[ActionParameters.Key("value")].toString().toBoolean()) {
                    SpotifyWidget.spotifyAppRemote!!.playerApi.skipPrevious().setResultCallback {
                        CoroutineScope(Dispatchers.IO).launch {
                            SpotifyWidget.update(context, glanceId)
                        }
                    }
                }

            "next" ->
                if (parameters[ActionParameters.Key("value")].toString().toBoolean()) {
                    SpotifyWidget.spotifyAppRemote!!.playerApi.skipNext().setResultCallback {
                        CoroutineScope(Dispatchers.IO).launch {
                            SpotifyWidget.update(context, glanceId)
                        }
                    }
                }

            "play_pause" -> {
                val playerApi = SpotifyWidget.spotifyAppRemote!!.playerApi
                if (parameters[ActionParameters.Key("value")] == "play") {
                    playerApi.resume().setResultCallback {
                        CoroutineScope(Dispatchers.IO).launch {
                            SpotifyWidget.update(context, glanceId)
                        }
                    }
                } else if (parameters[ActionParameters.Key("value")] == "pause") {
                    playerApi.pause().setResultCallback {
                        CoroutineScope(Dispatchers.IO).launch {
                            SpotifyWidget.update(context, glanceId)
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
