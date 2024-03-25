package xyz.immortalforest.widgets.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.request.ImageRequest
import coil.request.ImageResult
import com.spotify.protocol.types.ImageUri
import java.io.File


inline val Context.imageLoaderr: ImageLoader
    get() = ImageLoader.Builder(this)
        .diskCache {
            val cacheDir = File(this.externalCacheDir?.absolutePath.toString(), "album")
            cacheDir.mkdirs()
            DiskCache.Builder()
                .directory(cacheDir)
                .maxSizeBytes(1024 * 1024 * 69)
                .build()
        }
        .build()

fun extractUri(imageUri: ImageUri): String {
    return Regex("spotify:image:(.*)").find(imageUri.raw.toString())?.groups?.get(1)?.value
        ?: imageUri.raw.toString().replace("spotify:image:", "")
}

suspend fun ImageLoader.requestImage(context: Context, imageUri: ImageUri): ImageResult {
    val uri = extractUri(imageUri)
    val imageRequest = ImageRequest.Builder(context)
        .data("https://i.scdn.co/image/$uri")
        .bitmapConfig(Bitmap.Config.RGB_565)
        .decoderFactory(
            Decoder.Factory { result, options, _ ->
                return@Factory BitmapFactoryDecoder(result.source, options)
            }
        )
        .build()
    return this.execute(imageRequest)
}

fun ImageLoader.requestAsyncImage(
    context: Context,
    imageUri: ImageUri,
    action: (Drawable) -> Unit
) {
    val uri = extractUri(imageUri)
    val imageRequest = ImageRequest.Builder(context)
        .data("https://i.scdn.co/image/$uri")
        .bitmapConfig(Bitmap.Config.RGB_565)
        .decoderFactory(
            Decoder.Factory { result, options, _ ->
                return@Factory BitmapFactoryDecoder(result.source, options)
            }
        )
        .target {
            Log.i("Decoder", "Drawable reached!")
            action(it)
        }.build()
    this.enqueue(imageRequest)
}
