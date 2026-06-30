package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun MetasScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Text("Metas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
