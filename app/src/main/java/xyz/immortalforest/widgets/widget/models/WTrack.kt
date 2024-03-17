package xyz.immortalforest.widgets.widget.models

import com.spotify.protocol.types.Album
import com.spotify.protocol.types.Artist
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.Track

class WTrack() {
    var name: String = "empty"
    var uri: String = ""
    var imageUri: ImageUri = ImageUri("")
    var artists: List<Artist> = emptyList()
    var album: Album = Album("", "")

    constructor (track: Track) : this() {
        this.name = track.name
        this.uri = track.uri
        this.imageUri = track.imageUri
        this.artists = track.artists
        this.album = track.album
    }

    fun getArtists(): String {
        return artists.joinToString(", ") {
            it.name
        }
    }

}