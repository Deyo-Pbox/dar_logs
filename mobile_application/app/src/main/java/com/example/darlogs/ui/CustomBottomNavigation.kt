package com.example.darlogs.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.ui.theme.*

enum class NavItem(val id: Int, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    DASHBOARD(0, "Home", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    MY_WORK_LOGS(1, "Logs", Icons.Outlined.HistoryEdu, Icons.Filled.HistoryEdu),
    PENDING(2, "Pending", Icons.Outlined.HourglassEmpty, Icons.Filled.HourglassFull),
    COMPLETED(3, "Done", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
    ARCHIVE(4, "Archive", Icons.Outlined.Archive, Icons.Filled.Archive)
}

@Composable
fun CustomBottomNavigation(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    useLightMode: Boolean = false
) {
    // Glassmorphism effect colors
    val glassColor = if (useLightMode) {
        Color.White.copy(alpha = 0.92f) // White glass like in the screenshot
    } else {
        Color(0xFF0A1F14).copy(alpha = 0.75f)
    }
    
    val borderColor = if (useLightMode) {
        Color.White.copy(alpha = 0.4f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .navigationBarsPadding()
    ) {
        // Outer glow/shadow container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = if (useLightMode) Color.Black.copy(alpha = 0.1f) else Color.Black,
                    spotColor = if (useLightMode) Color(0xFF2D6A4F).copy(alpha = 0.3f) else BrandGreen.copy(alpha = 0.5f)
                ),
            color = Color.Transparent,
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                glassColor,
                                glassColor.copy(alpha = glassColor.alpha * 0.9f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (useLightMode) {
                                listOf(borderColor, borderColor.copy(alpha = 0.2f))
                            } else {
                                listOf(borderColor, borderColor.copy(alpha = borderColor.alpha * 0.3f))
                            }
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem.entries.forEach { item ->
                        val isSelected = selectedItem == item.id
                        
                        BottomNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onItemSelected(item.id) },
                            useLightMode = useLightMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    useLightMode: Boolean
) {
    val activeColor = if (useLightMode) MaterialTheme.colorScheme.primary else AccentGold
    val inactiveColor = if (useLightMode) MaterialTheme.colorScheme.onSurfaceVariant else TextMuted
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        label = "color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 44.dp else 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) {
                        activeColor.copy(alpha = if (useLightMode) 0.15f else 0.12f)
                    } else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale)
            )
        }
        
        if (isSelected) {
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
