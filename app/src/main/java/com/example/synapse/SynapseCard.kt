package com.example.synapse

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.synapse.ui.theme.BorderSilver
import com.example.synapse.ui.theme.SurfaceWhite

/**
 * Standard content card — crisp white surface, warm silver hairline border,
 * and a 2dp elevation shadow so it reads as a physical surface on the page.
 */
@Composable
fun SynapseCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border    = BorderStroke(1.dp, BorderSilver),
        elevation = CardDefaults.cardElevation(
            defaultElevation  = 2.dp,
            pressedElevation  = 1.dp
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        content()
    }
}