package it.di.unipi.sam636694.semelion.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class PdfBitmapConverter(
    private val context: Context
) {
    var renderer: PdfRenderer? = null

    public suspend fun pdfToBitmaps(contentUri: Uri): List<Bitmap>{
        return withContext(Dispatchers.IO){
            renderer?.close()

            context.contentResolver.openFileDescriptor(contentUri, "r")?.use { descriptor ->
                with(PdfRenderer(descriptor)){
                    renderer = this
                    return@withContext ( 0 until pageCount).map { index->
                        async {
                            openPage(index).use { page ->
                                val bitmap = createBitmap(page.width, page.height,Bitmap.Config.ARGB_8888)

                                // Prima riempi lo sfondo bianco
                                val canvas = Canvas(bitmap).apply {
                                    drawColor(android.graphics.Color.WHITE)
                                    drawBitmap(bitmap,0f,0f,null)
                                }

                                // Poi renderizza la pagina PDF sopra
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )

                                bitmap
                            }
                        }
                    }.awaitAll()
                }

            }
            return@withContext emptyList()
        }
    }
}