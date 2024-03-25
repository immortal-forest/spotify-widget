package xyz.immortalforest.widgets

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import xyz.immortalforest.widgets.ui.theme.WidgetsTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cacheDir = File(this.externalCacheDir?.absolutePath.toString(), "album")
        setContent {
            WidgetsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Content(
                        Modifier.wrapContentSize(),
                        {
                            it.longValue = getBytes()
                        }
                    ) {
                        cacheDir.deleteRecursively()
                        it.longValue = getBytes()
                    }
                }
            }
        }

    }

    @Composable
    private fun Content(
        modifier: Modifier,
        onRefresh: (MutableLongState) -> Unit,
        onClear: (MutableLongState) -> Unit
    ) {
        val cacheSize = remember {
            mutableLongStateOf(getBytes())
        }
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
            Spacer(modifier = modifier.size(0.dp, 6.dp))
            Text(
                text = "Cache size: ${convertBytes(cacheSize.longValue)}",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onRefresh(cacheSize) },
                    modifier = modifier,
                    colors = ButtonColors(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        Color.Gray,
                        Color.White
                    )
                ) {
                    Text(text = "Refresh")
                    Icon(Icons.Rounded.Refresh, "Refresh")
                }
                Spacer(modifier = modifier.size(20.dp, 0.dp))
                TextButton(
                    onClick = { onClear(cacheSize) },
                    modifier = modifier,
                    colors = ButtonColors(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        Color.Gray,
                        Color.White
                    )
                ) {
                    Text(text = "Clear")
                    Icon(Icons.Rounded.Clear, "Clear")
                }
            }
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

    private fun getBytes(): Long {
        return File(this.externalCacheDir?.absolutePath.toString(), "album").listFiles()
            ?.sumOf { file ->
                file.length()
            } ?: 0L
    }

    private fun convertBytes(sizeInBytes: Long): String {
        val units = arrayOf("B", "KiB", "MiB", "GiB")
        var power = 0
        var size = sizeInBytes.toDouble()
        while (size > 1024 && power < units.lastIndex) {
            size /= 1024
            power++
        }
        return String.format("%.2f %s", size, units[power])
    }
}
