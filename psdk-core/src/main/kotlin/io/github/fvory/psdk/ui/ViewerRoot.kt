package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import io.github.fvory.psdk.PdfSignatureViewer

@Composable
internal fun ViewerRoot(
    viewer: PdfSignatureViewer,
    modifier: Modifier = Modifier,
) {
    val state by viewer.state.collectAsState()
    Box(
        modifier
            .fillMaxSize()
            .background(Palette.background)
            .clipToBounds(),
    ) {
        when {
            state.isLoaded -> PageList(viewer, state)
            !state.isLoading -> EmptyState(viewer.labels, state.error)
        }
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Palette.accent)
        }
    }
}
