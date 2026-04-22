package it.di.unipi.sam636694.semelion

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import it.di.unipi.sam636694.semelion.utilities.PdfBitmapConverter
import java.io.File

@Composable
fun PdfViewerScreen(
    modifier: Modifier = Modifier,
    padding : PaddingValues
){
    val context = LocalContext.current
    val pdfUri = remember {
        val file = File(context.cacheDir, "Regole.pdf")
        context.assets.open("Regole.pdf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        Uri.fromFile(file)
    }
    var renderedPages by remember {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    val pdfBitmapConverter = remember {
        PdfBitmapConverter(context)
    }

    LaunchedEffect(pdfUri) {
        renderedPages = pdfBitmapConverter.pdfToBitmaps(pdfUri)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(renderedPages) { page ->
                PdfPage(
                    page = page,
                    modifier = modifier
                )
            }
        }
    }

}

@Composable
fun PdfPage(page:Bitmap, modifier:Modifier){
    Image(bitmap = page.asImageBitmap(), contentDescription = null,modifier = modifier.fillMaxSize().aspectRatio(page.width.toFloat()/page.height.toFloat()))
}