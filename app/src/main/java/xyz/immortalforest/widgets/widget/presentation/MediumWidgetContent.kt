package xyz.immortalforest.widgets.widget.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.spotify.protocol.types.PlayerRestrictions
import xyz.immortalforest.widgets.R
import xyz.immortalforest.widgets.widget.models.WTrack
import xyz.immortalforest.widgets.widget.util.SpotifyHelper

@Composable
fun MediumContent(
    spotifyHelper: SpotifyHelper,
    loading: Boolean,
    playerRestrictions: PlayerRestrictions,
    imageBitmap: Bitmap,
    isPaused: Boolean,
    containerColor: Color,
    containerTextColor: Color,
    iconColor: Color,
    track: WTrack,
    imageClick: () -> Unit,
    onSuccessAction: (Context, GlanceId) -> Unit,
    onErrorAction: () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(16.dp)
                .background(containerColor)
                .padding(4.dp)
                .wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize()
                ) {

                    Box(
                        modifier = GlanceModifier
                            .size(89.dp, 95.dp)
                            .cornerRadius(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(imageBitmap),
                            contentDescription = "Album art",
                            contentScale = ContentScale.FillBounds,
                            modifier = GlanceModifier.fillMaxSize()
                                .clickable(imageClick)
                        )
                    }
//                    Spacer(
//                        modifier = GlanceModifier.size(12.dp, 0.dp)
//                    )
                    Column(
                        modifier = GlanceModifier.fillMaxSize()
                            .padding(end = 12.dp, top = 5.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val trackNameLen = track.name.length
                        val artistsNameLen = track.getArtists().length

                        Text(
                            text = track.name,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(containerTextColor),
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = if (trackNameLen > 18) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Center
                                }
                            ),
                            modifier = GlanceModifier.size(width = 150.dp, height = 20.dp)
                                .padding(start = 12.dp)
                        )
                        Spacer(modifier = GlanceModifier.size(width = 0.dp, height = 4.dp))
                        Text(
                            text = track.getArtists(),
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(containerTextColor),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = if (artistsNameLen > 20) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.Center
                                },
                                textDecoration = TextDecoration.Underline
                            ),
                            modifier = GlanceModifier.size(width = 140.dp, height = 15.dp)
                                .padding(start = 12.dp)
                        )
                        Spacer(modifier = GlanceModifier.size(width = 0.dp, height = 10.dp))
                        Row(
                            modifier = GlanceModifier.padding(start = 12.dp)
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.prev_foreground),
                                contentDescription = "Previous",
                                modifier = GlanceModifier.size(40.dp)
                                    .cornerRadius(50.dp)
                                    .clickable {
                                        spotifyHelper.handleAction(
                                            action = "prev",
                                            value = playerRestrictions.canSkipPrev.toString(),
                                            loading = loading,
                                            onSuccess = onSuccessAction,
                                            onError = onErrorAction
                                        )
                                    },
                                colorFilter = ColorFilter.tint(ColorProvider(iconColor))
                            )
                            Image(
                                provider = ImageProvider(
                                    if (isPaused) {
                                        R.drawable.play_foreground
                                    } else {
                                        R.drawable.pause_foreground
                                    }
                                ),
                                contentDescription = "Play/Pause",
                                modifier = GlanceModifier.size(40.dp)
                                    .cornerRadius(50.dp)
                                    .clickable {
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
                                    },
                                colorFilter = ColorFilter.tint(ColorProvider(iconColor))
                            )
                            Image(
                                provider = ImageProvider(R.drawable.next_foreground),
                                contentDescription = "Next",
                                modifier = GlanceModifier.size(40.dp)
                                    .cornerRadius(50.dp)
                                    .clickable {
                                        spotifyHelper.handleAction(
                                            action = "next",
                                            value = playerRestrictions.canSkipNext.toString(),
                                            loading = loading,
                                            onSuccess = onSuccessAction,
                                            onError = onErrorAction
                                        )
                                    },
                                colorFilter = ColorFilter.tint(ColorProvider(iconColor))
                            )

                        }

                    }
                }

            }
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    provider = ImageProvider(R.drawable.spotify),
                    contentDescription = "Spotify logo",
                    modifier = GlanceModifier.size(12.dp)
                )
            }

        }
    }
}