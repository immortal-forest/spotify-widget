package xyz.immortalforest.widgets.widget.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.unit.ColorProvider

@Composable
fun Loading() {
    Column(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
                color = ColorProvider(Color.DarkGray)
        )
    }
}
