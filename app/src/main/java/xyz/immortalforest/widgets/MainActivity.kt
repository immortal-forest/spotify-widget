package xyz.immortalforest.widgets

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import xyz.immortalforest.widgets.ui.theme.WidgetsTheme

class MainActivity : ComponentActivity() {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WidgetsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Content()
                }
            }
        }

    }

    @Composable
    private fun Content() {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add the widget to your home screen!",
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Normal,
                fontSize = 16.sp,
                fontFamily = FontFamily.Default
            )
        }
    }


    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }

    }

    override fun onStart() {
        super.onStart()

        val clientId = "371ac845c4264fed92fc27a5224a4426"
        val redirectUri = "https://localhost:6969"

        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        // Just checking connection
        SpotifyAppRemote.connect(
            this@MainActivity,
            connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    spotifyAppRemote = appRemote
                    with(this@MainActivity) {
                        Toast.makeText(this, "Connected to Spotify app", Toast.LENGTH_SHORT).show()
                    }
                    spotifyAppRemote?.let {
                        SpotifyAppRemote.disconnect(it)
                    }
                }

                override fun onFailure(throwable: Throwable) {
                    // Something went wrong when attempting to connect! Handle errors here
                    Toast.makeText(
                        this@MainActivity,
                        "Error connecting to Spotify",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

    }
}
