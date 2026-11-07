package net.drehtuer.nextgallery.feature.settings

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

/**
 * Opens the system folder picker; [onPicked] gets the document id of the chosen folder, not when the user cancels. The
 * picker only chooses a path: no URI permission is taken, because the images keep coming from MediaStore (D55).
 */
@Composable
internal fun rememberFolderPicker(onPicked: (treeDocumentId: String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { onPicked(treeDocumentIdOf(it)) }
    }
    return remember(launcher) { { launcher.launch(null) } }
}

/** Other providers can also offer folders, such as a cloud app. Their ids are empty, which is not a usable folder. */
internal fun treeDocumentIdOf(uri: Uri): String =
    if (uri.authority == EXTERNAL_STORAGE_AUTHORITY) DocumentsContract.getTreeDocumentId(uri) else ""
