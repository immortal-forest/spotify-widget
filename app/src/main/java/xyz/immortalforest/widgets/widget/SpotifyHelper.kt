package xyz.immortalforest.widgets.widget

import android.content.Context
import androidx.glance.GlanceId
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpotifyHelper {

    private lateinit var context: Context
    private lateinit var glanceId: GlanceId

    fun new(
        context: Context, glanceId: GlanceId
    ) {
        this.context = context
        this.glanceId = glanceId
    }


    private val clientId: String = "371ac845c4264fed92fc27a5224a4426"
    private val redirectUri: String = "https://localhost:6969"

    private var actionRunning = false

    private val connectionParams = ConnectionParams.Builder(clientId)
        .setRedirectUri(redirectUri)
        .showAuthView(true)
        .build()

    private var spotifyAppRemote: SpotifyAppRemote? = null


    fun connectToSpotify(
        onConnect: (Context, GlanceId, SpotifyAppRemote) -> Unit,
        onError: (Context, GlanceId) -> Unit,
    ) {
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote?) {
                spotifyAppRemote = appRemote
                onConnect(context, glanceId, appRemote!!)
            }

            override fun onFailure(throwable: Throwable?) {
                onError(context, glanceId)
            }
        })
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    fun handleAction(
        action: String,
        value: String,
        loading: Boolean,
        onSuccess: (Context, GlanceId) -> Unit,
        onError: () -> Unit
    ) {
        if (loading && spotifyAppRemote == null) {
            CoroutineScope(Dispatchers.IO).launch {
                onError()
            }
        } else {
            if (actionRunning) {
                return
            } else {
                actionRunning = true
            }
            CoroutineScope(Dispatchers.IO).launch {
                onAction(
                    action,
                    value,
                    onSuccess
                )
            }
        }
    }


    private fun onAction(
        action: String,
        value: String,
        onSuccess: (Context, GlanceId) -> Unit
    ) {
        when (action) {
            "prev" -> {
                if (value.toBoolean()) {
                    spotifyAppRemote!!.playerApi.skipPrevious().setResultCallback {
                        onSuccess(context, glanceId)
                    }
                }
            }

            "next" -> {
                if (value.toBoolean()) {
                    spotifyAppRemote!!.playerApi.skipNext().setResultCallback {
                        onSuccess(context, glanceId)
                    }
                }
            }

            "play" -> {
                spotifyAppRemote!!.playerApi.resume().setResultCallback {
                    onSuccess(context, glanceId)
                }
            }

            "pause" -> {
                spotifyAppRemote!!.playerApi.pause().setResultCallback {
                    onSuccess(context, glanceId)
                }
            }

            else -> {}
        }
        actionRunning = false

    }


}