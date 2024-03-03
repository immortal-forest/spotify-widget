package xyz.immortalforest.widgets.widget.models

import com.spotify.protocol.types.Album
import com.spotify.protocol.types.Artist
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.Track

class WTrack(
    val name: String,
    val uri: String,
    val imageUri: ImageUri,
    val artists: List<Artist>,
    val album: Album,
) {
    constructor(
        name: String,
    ): this(
        name,
        "",
        ImageUri(""),
        emptyList(),
        Album("", "")
    )

    constructor(
        track: Track
    ): this(
        track.name,
        track.uri,
        track.imageUri,
        track.artists.toList(),
        track.album
    )

}