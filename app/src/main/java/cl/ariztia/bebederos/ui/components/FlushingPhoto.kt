package cl.ariztia.bebederos.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ariztia.bebederos.ui.theme.AppBackground
import cl.ariztia.bebederos.ui.theme.BrandRed
import cl.ariztia.bebederos.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Estados efímeros de decodificación; no representan datos guardados en Room. */
private sealed interface PhotoLoadState {
    data object Loading : PhotoLoadState
    data class Loaded(val bitmap: ImageBitmap) : PhotoLoadState
    data object Unavailable : PhotoLoadState
}

/**
 * Carga una fotografía desde su URI local sin bloquear el hilo principal.
 *
 * `produceState` reinicia la corrutina cuando cambia [uri] y convierte el resultado en estado
 * Compose; la UI se recompone para mostrar progreso, bitmap reducido o indisponibilidad segura.
 */
@Composable
fun FlushingPhoto(
    uri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by produceState<PhotoLoadState>(PhotoLoadState.Loading, uri) {
        value = withContext(Dispatchers.IO) {
            loadPreview(context, uri)
                ?.let { PhotoLoadState.Loaded(it) }
                ?: PhotoLoadState.Unavailable
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        when (val current = state) {
            PhotoLoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = BrandRed,
                strokeWidth = 2.dp
            )

            is PhotoLoadState.Loaded -> Image(
                bitmap = current.bitmap,
                contentDescription = "Fotografía del flushing",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            PhotoLoadState.Unavailable -> {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = TextSecondary
                )
                Text(
                    "Fotografía no disponible",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/** Decodifica una vista previa muestreada para limitar memoria y captura URIs/archivos inválidos. */
private fun loadPreview(context: Context, uriValue: String): ImageBitmap? =
    runCatching {
        val uri = Uri.parse(uriValue)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_PREVIEW_SIZE * 2 ||
            bounds.outHeight / sampleSize > MAX_PREVIEW_SIZE * 2
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val imageStream = context.contentResolver.openInputStream(uri) ?: return null
        imageStream.use {
            BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
        }
    }.getOrNull()

private const val MAX_PREVIEW_SIZE = 1_200
