package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SolarGold

/**
 * Fullscreen background displaying real solar system installation:
 * high-efficiency monocrystalline solar panels, wall-mounted hybrid inverter station,
 * and technician diagnostic tools with a cinematic dark gradient overlay.
 */
@Composable
fun SolarAuthBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060B14))
    ) {
        // 1. Real Solar System & Technician Tools Background Photo
        Image(
            painter = painterResource(id = R.drawable.img_solar_bg),
            contentDescription = "Solar panels, inverter and technician tools installation background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.90f
        )

        // 2. Subtle Sun & Solar Energy Radiance Glow Layer (Top Right)
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFA000),
                            Color(0x22FFD54F),
                            Color.Transparent
                        )
                    )
                )
        )

        // 3. Cinematic Vignette Gradient for High Legibility & Contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD060B14), // Deep obsidian sky overlay
                            Color(0xBF081222), // Subtle panel mid-visibility
                            Color(0xF5060A14)  // Grounding dark slate for forms & tools
                        )
                    )
                )
        )

        // 4. Foreground content slot
        content()
    }
}

/**
 * Official Solarfix Welcome Logo display:
 * Renders the official user-provided Solarfix logo (Sunburst, Dual Solar Panels,
 * Inverter Unit, and "SOLARFIX - My Solar System Technician" branding) with
 * golden accent borders and certified trust badge.
 */
@Composable
fun SolarfixWelcomeLogo(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String = "Certified Solar Diagnostics & Marketplace"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Official Transparent Solarfix Logo (Logo Only - No Background Box)
        Image(
            painter = painterResource(id = R.drawable.solarfix_logo),
            contentDescription = "Solarfix Logo - My Solar System Technician",
            modifier = Modifier
                .width(280.dp)
                .height(96.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Official Trust & Certified Seal
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x2600E5FF))
                .border(1.dp, Color(0x5500E5FF), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "My Solar System Technician • Certified",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF80D8FF)
            )
        }
    }
}
