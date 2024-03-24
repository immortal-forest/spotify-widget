package xyz.immortalforest.widgets.widget.models

import android.graphics.Bitmap
import com.spotify.protocol.types.ImageUri

data class WImage(
        val uri: ImageUri = ImageUri(""),
        val bitmap: Bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
)
