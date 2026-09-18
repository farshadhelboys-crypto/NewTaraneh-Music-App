package com.newtaraneh.music.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

object BrandAssets {
    fun logoBitmap(context: android.content.Context): ImageBitmap? {
        return decodeAsset(context, "logo.b64")
    }

    fun bannerBitmap(context: android.content.Context): ImageBitmap? {
        return decodeAsset(context, "banner.b64")
    }

    private fun decodeAsset(context: android.content.Context, name: String): ImageBitmap? {
        return try {
            val text = context.assets.open(name).bufferedReader().use { it.readText() }
            val bytes = Base64.decode(text.trim(), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun rememberLogoBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember { BrandAssets.logoBitmap(context) }
}

@Composable
fun rememberBannerBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember { BrandAssets.bannerBitmap(context) }
}
