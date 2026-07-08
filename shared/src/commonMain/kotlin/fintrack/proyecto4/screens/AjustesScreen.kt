package fintrack.proyecto4.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.theme.AppColors
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import androidx.compose.material.icons.filled.ArrowBackIosNew

@Composable
fun AjustesScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val c = LocalAppColors.current

    var notificaciones by remember { mutableStateOf(true) }
    var biometrico by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
    ) {
        // ── TopBar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.surfaceSecondary)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Volver",
                    tint = c.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Ajustes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary
            )
        }
        Spacer(Modifier.height(8.dp))

        // ── Tarjeta de perfil ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(FinTrackColors.GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("AV", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Ana Vargas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary)
                Text("ana.vargas@gmail.com", fontSize = 13.sp, color = c.textSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Editar perfil →", fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, color = FinTrackColors.GreenPrimary,
                    modifier = Modifier.clickable { }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── PERFIL ───────────────────────────────────────────────────────────
        SectionHeader("PERFIL", c)
        SectionCard(c) {
            ItemRow("Nombre visible", "Ana Vargas", c)
            RowDivider(c)
            ItemRow("Correo", "ana.vargas@gmail.com", c)
            RowDivider(c)
            ItemRow("Moneda principal", "CRC (₡)", c)
        }

        Spacer(Modifier.height(20.dp))

        // ── CONFIGURACIÓN FINANCIERA ─────────────────────────────────────────
        SectionHeader("CONFIGURACIÓN FINANCIERA", c)
        SectionCard(c) {
            ItemRow("Ingreso mensual estimado", "₡970.000", c)
            RowDivider(c)
            ItemRow("Inicio del período", "1ro de cada mes", c)
            RowDivider(c)
            ItemRow("Método de pago frecuente", "Tarjeta", c)
        }

        Spacer(Modifier.height(20.dp))

        // ── PREFERENCIAS ─────────────────────────────────────────────────────
        SectionHeader("PREFERENCIAS", c)
        SectionCard(c) {
            ToggleRow("Notificaciones push", notificaciones, { notificaciones = it }, c)
            RowDivider(c)
            ToggleRow("Acceso biométrico", biometrico, { biometrico = it }, c)
            RowDivider(c)
            ToggleRow("Tema oscuro", isDarkTheme, { onToggleTheme() }, c)
        }

        Spacer(Modifier.height(28.dp))

        // ── Cerrar sesión ────────────────────────────────────────────────────
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, FinTrackColors.ErrorColor)
        ) {
            Text("Cerrar sesión", color = FinTrackColors.ErrorColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ─── Componentes internos ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, c: AppColors) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = c.textSecondary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SectionCard(c: AppColors, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
    ) {
        content()
    }
}

@Composable
private fun ItemRow(label: String, value: String, c: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = c.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 14.sp, color = c.textSecondary)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = c.border,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, c: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = c.textPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FinTrackColors.GreenPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = c.divider
            )
        )
    }
}

@Composable
private fun RowDivider(c: AppColors) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = c.divider
    )
}
