package fintrack.proyecto4.screens

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
import fintrack.proyecto4.theme.FinTrackColors

// ─── Colores adaptativos ──────────────────────────────────────────────────────

private data class AjustesColors(
    val bg: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textValue: Color,
    val divider: Color,
    val sectionHeader: Color,
    val avatarBg: Color,
    val chevron: Color
)

private fun ajustesColors(isDark: Boolean) = AjustesColors(
    bg            = if (isDark) Color(0xFF080E1A) else Color(0xFFF1F5F9),
    surface       = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF),
    textPrimary   = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
    textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
    textValue     = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
    divider       = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
    sectionHeader = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
    avatarBg      = if (isDark) FinTrackColors.GreenDark else FinTrackColors.GreenPrimary,
    chevron       = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
)

// ─── Pantalla principal ───────────────────────────────────────────────────────

@Composable
fun AjustesScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onCerrarSesion: () -> Unit = {}
) {
    val c = ajustesColors(isDarkTheme)

    var notificaciones by remember { mutableStateOf(true) }
    var biometrico by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header título ──────────────────────────────────────────────────
        Text(
            text = "Ajustes",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
        )

        // ── Tarjeta de perfil ──────────────────────────────────────────────
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
                    .background(c.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AV",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ana Vargas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary
                )
                Text(
                    text = "ana.vargas@gmail.com",
                    fontSize = 13.sp,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Editar perfil →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinTrackColors.GreenPrimary,
                    modifier = Modifier.clickable { }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Sección PERFIL ─────────────────────────────────────────────────
        SectionHeader("PERFIL", c.sectionHeader)
        SectionCard(c.surface) {
            ItemRow("Nombre visible", "Ana Vargas", c)
            RowDivider(c.divider)
            ItemRow("Correo", "ana.vargas@gmail.com", c)
            RowDivider(c.divider)
            ItemRow("Moneda principal", "CRC (₡)", c)
        }

        Spacer(Modifier.height(20.dp))

        // ── Sección CONFIGURACIÓN FINANCIERA ──────────────────────────────
        SectionHeader("CONFIGURACIÓN FINANCIERA", c.sectionHeader)
        SectionCard(c.surface) {
            ItemRow("Ingreso mensual estimado", "₡970.000", c)
            RowDivider(c.divider)
            ItemRow("Inicio del período", "1ro de cada mes", c)
            RowDivider(c.divider)
            ItemRow("Método de pago frecuente", "Tarjeta", c)
        }

        Spacer(Modifier.height(20.dp))

        // ── Sección PREFERENCIAS ───────────────────────────────────────────
        SectionHeader("PREFERENCIAS", c.sectionHeader)
        SectionCard(c.surface) {
            ToggleRow(
                label = "Notificaciones push",
                checked = notificaciones,
                onCheckedChange = { notificaciones = it },
                c = c
            )
            RowDivider(c.divider)
            ToggleRow(
                label = "Acceso biométrico",
                checked = biometrico,
                onCheckedChange = { biometrico = it },
                c = c
            )
            RowDivider(c.divider)
            ToggleRow(
                label = "Tema oscuro",
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
                c = c
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Botón cerrar sesión ────────────────────────────────────────────
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = FinTrackColors.ErrorColor
            )
        ) {
            Text(
                text = "Cerrar sesión",
                color = FinTrackColors.ErrorColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ─── Componentes internos ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SectionCard(surface: Color, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
    ) {
        content()
    }
}

@Composable
private fun ItemRow(label: String, value: String, c: AjustesColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = c.textPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = c.textValue
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = c.chevron,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    c: AjustesColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = c.textPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FinTrackColors.GreenPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (checked) FinTrackColors.GreenPrimary else c.divider
            )
        )
    }
}

@Composable
private fun RowDivider(color: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = color
    )
}
