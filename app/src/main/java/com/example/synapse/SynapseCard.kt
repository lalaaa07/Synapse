package com.example.synapse

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.synapse.ui.theme.BorderDark
import com.example.synapse.ui.theme.SurfaceDark

@Composable
fun SynapseCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}