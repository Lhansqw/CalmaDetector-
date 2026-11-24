package com.example.calma.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.calma.R
import com.example.calma.ui.theme.Blush
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.Pink

enum class DashboardTab(val title: String, @DrawableRes val icon: Int) {
    HOME("Inicio", R.drawable.ic_home),
    ASSISTANT("Asistente", R.drawable.ic_chat),
    CALIBRATION("Calibración", R.drawable.ic_tune),
    HISTORY("Historial", R.drawable.ic_history)
}

/** Barra de 4 pestañas cuadradas con relieve. La elegida va hundida y en rosa. */
@Composable
fun BottomTabBar(
    selected: DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    badgeOn: Set<DashboardTab> = emptySet()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blush)
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DashboardTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .neumorphic(14.dp, elevation = 4.dp, pressed = isSelected)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics {
                            contentDescription = tab.title
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        tint = if (isSelected) Pink else InkSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (tab in badgeOn) {
                    Box(
                        modifier = Modifier
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Pink)
                    )
                }
            }
        }
    }
}
