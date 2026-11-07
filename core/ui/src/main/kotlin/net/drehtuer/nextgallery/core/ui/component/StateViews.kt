package net.drehtuer.nextgallery.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.core_ui_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

/** [action] is what the user can do about it, shown below the text. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    @DrawableRes icon: Int? = null,
    action: @Composable () -> Unit = {},
) {
    CenteredMessage(title, detail, modifier, icon, actions = action)
}

@Composable
fun ErrorState(title: String, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(title, message, modifier, R.drawable.core_ui_ic_warning, MaterialTheme.colorScheme.error) {
        Button(onClick = onRetry) { Text(stringResource(R.string.core_ui_retry)) }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    detail: String?,
    modifier: Modifier,
    @DrawableRes icon: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actions: @Composable () -> Unit = {},
) {
    val dimensions = NextGalleryTheme.dimensions
    Column(
        modifier = modifier.fillMaxSize().padding(dimensions.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.let { Icon(painterResource(it), contentDescription = null, tint = iconTint) }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        detail?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        actions()
    }
}
