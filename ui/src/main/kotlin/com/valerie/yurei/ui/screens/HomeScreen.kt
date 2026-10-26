// ui/screens/HomeScreen.kt
package com.valerie.yurei.ui.screens
import com.valerie.yurei.ui.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerie.yurei.ui.navigation.RootNav
import com.valerie.yurei.ui.theme.YureiTitleFont   // cf. Type.kt (section 3)


@Composable
fun HomeScreen(
    onStartGame: () -> Unit,
    highScore: Int = 0,
    memoryUnlocked: Boolean = false,
    ready: Boolean = true,
    hasSavedGame: Boolean = false,
    onContinue: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize()) {
        // 1) Fond illustré
        Image(
            painter = painterResource(id = R.drawable.dragon_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2) Léger dégradé pour le contraste (haut clair, centre neutre, bas légèrement sombre)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x33FFFFFF),
                        0.45f to Color.Transparent,
                        1f to Color(0x55000000)
                    )
                )
        )

        // Bouton Paramètres (coin supérieur droit)
        onOpenSettings?.let { openSettings ->
            IconButton(
                onClick = openSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = Color.White
                )
            }
        }

        // 3) Mise en page en tiers : Titre (tiers supérieur), vide (tiers milieu), bouton (tiers inférieur)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Tiers 1 — Titre
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "YŪREI",
                    // Police “japonaise” si dispo, sinon Serif en fallback :
                    fontFamily = YureiTitleFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 66.sp,
                    color = Color.Black,
                    lineHeight = 56.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .shadow(2.dp, spotColor = Color(0x22000000))
                )
            }

            // Tiers 2 — espace “respiration”
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("I · LE LAC DES LANTERNES", color = Color.White, fontSize = 17.sp,
                        modifier = Modifier.background(Color(0x880E1B2A), RoundedCornerShape(12.dp)).padding(12.dp))
                    Text(if (memoryUnlocked) "La lanterne offerte · souvenir retrouvé" else "20 âmes · 3 minutes",
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.background(Color(0x880E1B2A), RoundedCornerShape(8.dp)).padding(8.dp))
                }
            }

            // Tiers 3 — Bouton "Commencer"
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasSavedGame) {
                    Button(onClick = onContinue, enabled = ready) { Text("Continuer la partie") }
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    enabled = ready,
                    onClick = onStartGame,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xEEFFFFFF),
                        contentColor = Color(0xFF111111)
                    ),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                ) {
                    Text(if (!ready) "Chargement…" else "Commencer la partie", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                }
            }
        }
    }
}
