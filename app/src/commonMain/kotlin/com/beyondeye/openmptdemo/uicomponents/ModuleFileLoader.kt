package com.beyondeye.openmptdemo.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/**
 * Supported tracker module file extensions for libopenmpt.
 */
private val SUPPORTED_EXTENSIONS = listOf(
    "mptm", "mod", "s3m", "xm", "it", "667", "669", "amf", "ams", "c67",
    "cba", "dbm", "digi", "dmf", "dsm", "dsym", "dtm", "etx", "far", "fc",
    "fc13", "fc14", "fmt", "fst", "ftm", "imf", "ims", "ice", "j2b", "m15",
    "mdl", "med", "mms", "mt2", "mtm", "mus", "nst", "okt", "plm", "psm",
    "pt36", "ptm", "puma", "rtm", "sfx", "sfx2", "smod", "st26", "stk",
    "stm", "stx", "stp", "symmod", "tcb", "gmc", "gtk", "gt2", "ult",
    "unic", "wow", "xmf", "gdm", "mo3", "oxm", "umx", "xpk", "ppm", "mmcmp"
)

private const val MOD_ARCHIVE_URL = "https://modarchive.org/"
private const val URL_TAG = "URL"

/**
 * A composable component for loading tracker module files.
 * 
 * Provides:
 * - A button to load a bundled sample MOD file from resources
 * - A button to open a file picker dialog to load a MOD file from the file system
 * - A clickable link to modarchive.org for downloading more modules
 *
 * @param onLoadSampleFile Callback invoked when the user clicks "Load Sample MOD File"
 * @param onLoadFileBytes Callback invoked with the file bytes when a file is selected from the picker
 * @param isLoading Whether a file is currently being loaded
 * @param modifier Modifier for the component
 */
@Composable
fun ModuleFileLoader(
    onLoadSampleFile: () -> Unit,
    onLoadFileBytes: (ByteArray) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    
    // File picker launcher using FileKit Compose
    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = SUPPORTED_EXTENSIONS),
        mode = FileKitMode.Single,
        title = "Select Module File"
    ) { file ->
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                onLoadFileBytes(bytes)
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Load Sample MOD File button
        Button(
            onClick = onLoadSampleFile,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Load Sample MOD File")
        }
        
        // Load from File button
        OutlinedButton(
            onClick = { filePicker.launch() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Load from File...")
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Clickable link to modarchive.org
        ModArchiveLink(
            onLinkClick = { uriHandler.openUri(MOD_ARCHIVE_URL) }
        )
    }
}

/**
 * A clickable text link to modarchive.org.
 */
@Composable
private fun ModArchiveLink(
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    
    val annotatedString = buildAnnotatedString {
        append("Download more music from ")
        pushStringAnnotation(tag = URL_TAG, annotation = MOD_ARCHIVE_URL)
        withStyle(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("modarchive.org")
        }
        pop()
    }
    
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = URL_TAG,
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                onLinkClick()
            }
        }
    )
}
