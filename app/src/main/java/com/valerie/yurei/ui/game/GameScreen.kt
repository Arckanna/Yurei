package com.valerie.yurei.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

import androidx.compose.ui.unit.IntSize

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


import androidx.compose.ui.layout.onSizeChanged


// Palette Yūrei (bleu lunaire & violet brumeux)
private object YureiColors {
    val NightBlue = Color(0xFF0B1024)   // bleu nuit
    val DeepIndigo = Color(0xFF141A3C)  // indigo profond
    val MistViolet = Color(0xFF5A46A8)  // violet brumeux
    val MoonGlow  = Color(0xFFD5D7FF)   // lueur froide
    val CyanMist  = Color(0xFF6FC6FF)   // brume cyan légère
}

// Un simple “blob” de brume
private data class FogBlob(
    val base: Offset,   // centre de base (en pixels, relatif à la taille du canvas au moment du calc)
    val radius: Float,  // rayon
    val phase: Float,   // déphasage d’animation
    val speed: Float,   // vitesse (px/s)
    val color: Color,   // teinte
    val alpha: Float    // opacité max
)

// Génère une petite constellation de blobs
private fun generateBlobs(w: Float, h: Float, count: Int, seed: Long = 42L): List<FogBlob> {
    val rnd = Random(seed)
    val minR = (w.coerceAtMost(h)) * 0.10f
    val maxR = (w.coerceAtMost(h)) * 0.30f
    return List(count) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h
        val r = minR + rnd.nextFloat() * (maxR - minR)
        val ph = rnd.nextFloat() * (2f * PI).toFloat()
        val sp = 6f + rnd.nextFloat() * 16f // vitesse douce
        val c  = if (rnd.nextBoolean()) YureiColors.MistViolet else YureiColors.CyanMist
        val a  = 0.10f + rnd.nextFloat() * 0.20f
        FogBlob(Offset(x, y), r, ph, sp, c, a)
    }
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        FogBackground()

        // Placeholder HUD (tu mettras ici ton Canvas de jeu plus tard)
        Text(
            text = "Yūrei",
            color = YureiColors.MoonGlow.copy(alpha = 0.9f),
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun FogBackground() {
    // Taille du canvas (mise à jour via onSizeChanged)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Animation “respiration” globale
    val transition = rememberInfiniteTransition(label = "fog")
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Halo rotation
    val haloRot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo"
    )

    // Un “temps” qui progresse en continu (0f -> 1f -> 2f ...), utilisable dans le draw
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10_000f, // valeur arbitraire, on boucle
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000), // lent
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Génère les blobs quand la taille change (pas dans le draw!)
    val blobs = remember(canvasSize) {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w > 0f && h > 0f) generateBlobs(w, h, count = 7, seed = 1991L) else emptyList()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        val w = size.width
        val h = size.height

        // Fond
        drawRect(
            brush = Brush.verticalGradient(
                0f to YureiColors.NightBlue,
                1f to YureiColors.DeepIndigo
            ),
            size = size
        )

        // Blobs
        blobs.forEachIndexed { i, b ->
            drawFogBlob(
                blob = b,
                t = time + i * 0.37f,
                globalScale = pulse
            )
        }

        // Halo (enso)
        withTransform({
            rotate(degrees = haloRot, pivot = center)
        }) {
            val haloR = (w.coerceAtMost(h)) * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        YureiColors.MoonGlow.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = haloR
                ),
                radius = haloR,
                center = center,
                blendMode = BlendMode.Screen
            )
            drawCircle(
                color = YureiColors.MoonGlow.copy(alpha = 0.10f),
                radius = haloR * 0.72f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = haloR * 0.008f),
                blendMode = BlendMode.Screen
            )
        }
    }
}


private fun DrawScope.drawFogBlob(
    blob: FogBlob,
    t: Float,
    globalScale: Float
) {
    // Légère dérive circulaire autour du centre de base
    val driftRadius = blob.radius * 0.15f
    val x = blob.base.x + driftRadius * cos(blob.phase + t * blob.speed / 160f)
    val y = blob.base.y + driftRadius * sin(blob.phase + t * blob.speed / 200f)

    val r = blob.radius * (0.86f + 0.14f * sin(blob.phase + t * 0.4f)) * globalScale

    // Dégradé radial doux (Screen) → brume lumineuse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                blob.color.copy(alpha = blob.alpha),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = r
        ),
        radius = r,
        center = Offset(x, y),
        blendMode = BlendMode.Screen
    )
}


