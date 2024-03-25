package xyz.immortalforest.widgets.widget.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.spotify.protocol.types.PlayerRestrictions
import xyz.immortalforest.widgets.R
import xyz.immortalforest.widgets.widget.util.SpotifyHelper

@Composable
fun SmallContent(
    spotifyHelper: SpotifyHelper,
    loading: Boolean,
    playerRestrictions: PlayerRestrictions,
    imageBitmap: Bitmap,
    isPaused: Boolean,
    containerColor: Color,
    iconColor: Color,
    imageClick: () -> Unit,
    onSuccessAction: (Context, GlanceId) -> Unit,
    onErrorAction: () -> Unit,
) {

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier.size(150.dp)
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .cornerRadius(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(imageBitmap),
                    contentDescription = "Album art",
                    modifier = GlanceModifier.fillMaxSize()
                        .clickable(imageClick),
                    contentScale = ContentScale.FillBounds
                )
            }
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = GlanceModifier.size(16.dp)
                        .padding(end = 4.dp, top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.spotify),
                        contentDescription = "Spotify icon",
                    )
                }
            }
        }
    }

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = GlanceModifier
                .background(
                    containerColor
                ).size(130.dp, 50.dp).cornerRadius(12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize()
                    .padding(start = 2.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = GlanceModifier.size(40.dp).clickable {
                        spotifyHelper.handleAction(
                            action = "prev",
                            value = playerRestrictions.canSkipPrev.toString(),
                            loading = loading,
                            onSuccess = onSuccessAction,
                            onError = onErrorAction
                        )
                    }
                        .cornerRadius(16.dp),
                    provider = ImageProvider(R.drawable.prev_foreground),
                    contentDescription = "Previous",
                    colorFilter = ColorFilter.tint(ColorProvider(iconColor))
                )
                Image(
                    modifier = GlanceModifier.size(40.dp).clickable {
                        spotifyHelper.handleAction(
                            action = if (isPaused) {
                                "play"
                            } else {
                                "pause"
                            },
                            value = "",
                            loading = loading,
                            onSuccess = onSuccessAction,
                            onError = onErrorAction
                        )
                    }
                        .cornerRadius(16.dp),
                    provider = ImageProvider(
                        if (isPaused) {
                            R.drawable.play_foreground
                        } else {
                            R.drawable.pause_foreground
                        }
                    ),
                    contentDescription = "Play",
                    colorFilter = ColorFilter.tint(ColorProvider(iconColor))
                )
                Image(
                    modifier = GlanceModifier.size(40.dp).clickable {
                        spotifyHelper.handleAction(
                            action = "next",
                            value = playerRestrictions.canSkipNext.toString(),
                            loading = loading,
                            onSuccess = onSuccessAction,
                            onError = onErrorAction
                        )
                    }
                        .cornerRadius(16.dp),
                    provider = ImageProvider(R.drawable.next_foreground),
                    contentDescription = "Next",
                    colorFilter = ColorFilter.tint(ColorProvider(iconColor))
                )
            }

        }

    }
}
