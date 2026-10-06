// The living pixel world behind every screen: parallax biomes, a sky that turns like a dial, and weather per mode.
// Switching modes makes the camera travel across the world to that mode's biome.
package com.example.geoseek.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** How many world cells fit across the screen. Everything is drawn on this grid. */
private const val COLS = 150f
private const val BIOME_WIDTH = 320f
private val WORLD_WIDTH = BIOME_WIDTH * Biome.entries.size
private const val DAY_SECONDS = 150f
private const val ROAM_SPEED = 7f

private class WorldState {
    var clock by mutableFloatStateOf(0f)
    var tod by mutableFloatStateOf(0.42f)
    var cam by mutableFloatStateOf(BIOME_WIDTH * 0.35f)

    fun step(dt: Float, scene: Scene) {
        clock += dt
        val targetTime = scene.timeOfDay
        tod = if (targetTime == null) {
            (tod + dt / DAY_SECONDS) % 1f
        } else {
            var d = targetTime - tod
            d -= round(d) // turn the dial the short way round
            (tod + d * (1f - exp(-dt * 1.1f))).mod(1f)
        }
        if (scene.roam) {
            cam += dt * ROAM_SPEED
        } else {
            val center = (scene.biome.ordinal + 0.35f) * BIOME_WIDTH + sin(clock * 0.15f) * 10f
            val target = center + round((cam - center) / WORLD_WIDTH) * WORLD_WIDTH
            cam += (target - cam) * (1f - exp(-dt * 1.3f))
        }
    }
}

@Composable
fun WorldBackground(scene: Scene, modifier: Modifier = Modifier, dim: Float = 0f) {
    val world = remember { WorldState() }
    val currentScene by rememberUpdatedState(scene)

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                world.step(dt, currentScene)
            }
        }
    }

    val fade = tween<Float>(1600)
    val dimA by animateFloatAsState(dim, tween(700), label = "dim")
    val leaves by animateFloatAsState(scene.leaves, fade, label = "leaves")
    val snow by animateFloatAsState(scene.snow, fade, label = "snow")
    val aurora by animateFloatAsState(scene.aurora, fade, label = "aurora")
    val fireflies by animateFloatAsState(scene.fireflies, fade, label = "fireflies")
    val sparkles by animateFloatAsState(scene.sparkles, fade, label = "sparkles")

    Canvas(modifier) {
        val cell = size.width / COLS
        val rows = size.height / cell
        val t = world.tod
        val cam = world.cam
        val clock = world.clock
        val day = sample(DAY_KEYS, DAY_LIGHT, t)
        val night = 1f - day
        val skyTop = sampleColor(SKY_KEYS, SKY_TOP, t)
        val skyBottom = sampleColor(SKY_KEYS, SKY_BOTTOM, t)
        val pivot = Offset(size.width / 2f, size.height * 0.62f)

        drawSky(skyTop, skyBottom)
        drawStars(pivot, t, clock, night, cell)
        if (sparkles > 0.01f) drawSparkles(clock, cell, sparkles)
        if (aurora > 0.01f) drawAurora(clock, cell, rows, aurora * night)
        drawCelestial(pivot, t, cell, day)
        drawClouds(clock, cam, cell, rows, day)

        LAYERS.forEachIndexed { index, spec ->
            drawLayer(index, spec, cam, clock, cell, rows, day, skyBottom)
        }

        if (fireflies > 0.01f) drawFireflies(clock, cam, cell, fireflies * night)
        if (leaves > 0.01f) drawLeaves(clock, cam, cell, leaves, day)
        if (snow > 0.01f) drawSnow(clock, cam, cell, snow)

        // A soft scrim at the top keeps status-bar icons and titles readable on bright skies.
        drawRect(Brush.verticalGradient(0f to Color(0x55000000), 0.22f to Color.Transparent))
        if (dimA > 0f) drawRect(Color.Black.copy(alpha = dimA))
    }
}

// ---------------------------------------------------------------------------------------------
// Sky

private val SKY_KEYS = floatArrayOf(0f, 0.2f, 0.27f, 0.36f, 0.64f, 0.73f, 0.8f, 1f)
private val NIGHT_TOP = Color(0xFF060920)
private val NIGHT_BOTTOM = Color(0xFF1A2252)
private val SKY_TOP = listOf(
    NIGHT_TOP, NIGHT_TOP, Color(0xFF2E3F86), Color(0xFF2F6FD6),
    Color(0xFF2F6FD6), Color(0xFF3B2560), NIGHT_TOP, NIGHT_TOP,
)
private val SKY_BOTTOM = listOf(
    NIGHT_BOTTOM, NIGHT_BOTTOM, Color(0xFFF4A07A), Color(0xFF9FD4F7),
    Color(0xFF9FD4F7), Color(0xFFF2774B), NIGHT_BOTTOM, NIGHT_BOTTOM,
)
private val DAY_KEYS = floatArrayOf(0f, 0.2f, 0.32f, 0.68f, 0.8f, 1f)
private val DAY_LIGHT = floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f)

private fun segment(keys: FloatArray, t: Float): Pair<Int, Float> {
    for (i in 0 until keys.size - 1) {
        if (t <= keys[i + 1]) return i to ((t - keys[i]) / (keys[i + 1] - keys[i])).coerceIn(0f, 1f)
    }
    return keys.size - 2 to 1f
}

private fun sample(keys: FloatArray, values: FloatArray, t: Float): Float {
    val (i, f) = segment(keys, t)
    val s = f * f * (3 - 2 * f)
    return values[i] + (values[i + 1] - values[i]) * s
}

private fun sampleColor(keys: FloatArray, values: List<Color>, t: Float): Color {
    val (i, f) = segment(keys, t)
    return lerp(values[i], values[i + 1], f * f * (3 - 2 * f))
}

private fun DrawScope.drawSky(top: Color, bottom: Color) {
    // Banded rather than smooth, so the sky reads as pixel art.
    val bands = 36
    val bandH = size.height * 0.8f / bands
    for (b in 0 until bands) {
        drawRect(lerp(top, bottom, b / (bands - 1f)), Offset(0f, b * bandH), Size(size.width, bandH + 1f))
    }
    drawRect(bottom, Offset(0f, bands * bandH), Size(size.width, size.height - bands * bandH))
}

private fun DrawScope.drawStars(pivot: Offset, t: Float, clock: Float, night: Float, cell: Float) {
    if (night < 0.02f) return
    val reach = maxOf(size.width, size.height)
    // The whole starfield turns with the time of day, around the same pivot as the sun and moon.
    val turn = t * 2f * PI.toFloat()
    for (i in 0 until 240) {
        val r = sqrt(hash(i, 1)) * reach
        val a = hash(i, 2) * 2f * PI.toFloat() + turn
        val x = snap(pivot.x + r * cos(a), cell)
        val y = snap(pivot.y + r * sin(a), cell)
        if (x < -cell || x > size.width || y < -cell || y > size.height * 0.75f) continue
        val twinkle = 0.55f + 0.45f * sin(clock * (1.5f + hash(i, 3) * 2.5f) + i)
        val alpha = (night * twinkle).coerceIn(0f, 1f)
        val color = if (hash(i, 4) > 0.85f) Color(0xFFFFE6A8) else Color(0xFFE9EEFF)
        drawRect(color.copy(alpha = alpha), Offset(x, y), Size(cell, cell))
        if (hash(i, 5) > 0.9f) {
            val arm = color.copy(alpha = alpha * 0.5f)
            drawRect(arm, Offset(x - cell, y), Size(cell, cell))
            drawRect(arm, Offset(x + cell, y), Size(cell, cell))
            drawRect(arm, Offset(x, y - cell), Size(cell, cell))
            drawRect(arm, Offset(x, y + cell), Size(cell, cell))
        }
    }
}

private fun DrawScope.drawSparkles(clock: Float, cell: Float, amount: Float) {
    for (i in 0 until 28) {
        val pulse = sin(clock * 1.7f + i * 1.31f).coerceAtLeast(0f).pow(3)
        if (pulse < 0.05f) continue
        val x = snap(hash(i, 21) * size.width, cell)
        val y = snap(hash(i, 22) * size.height * 0.45f, cell)
        val c = Color(0xFFFFD86B).copy(alpha = pulse * amount)
        drawRect(c, Offset(x, y), Size(cell, cell))
        val arm = c.copy(alpha = pulse * amount * 0.6f)
        val len = if (pulse > 0.6f) 2 else 1
        for (k in 1..len) {
            drawRect(arm, Offset(x - k * cell, y), Size(cell, cell))
            drawRect(arm, Offset(x + k * cell, y), Size(cell, cell))
            drawRect(arm, Offset(x, y - k * cell), Size(cell, cell))
            drawRect(arm, Offset(x, y + k * cell), Size(cell, cell))
        }
    }
}

private fun DrawScope.drawAurora(clock: Float, cell: Float, rows: Float, amount: Float) {
    if (amount < 0.02f) return
    val cols = (size.width / cell).toInt() + 1
    val green = Color(0xFF5CF2A8)
    val violet = Color(0xFF9D7BFF)
    for (c in 0 until cols) {
        val wave = sin(c * 0.07f + clock * 0.35f) * 0.05f + sin(c * 0.023f - clock * 0.22f) * 0.035f
        val center = rows * (0.2f + wave)
        val height = 14f + 8f * sin(c * 0.05f + clock * 0.5f)
        val shimmer = 0.6f + 0.4f * sin(c * 0.2f + clock * 1.3f)
        val segments = 6
        for (s in 0 until segments) {
            val f = s / (segments - 1f)
            val color = lerp(green, violet, f)
            val alpha = amount * shimmer * (1f - f) * 0.32f
            val y = snap((center - f * height) * cell, cell)
            drawRect(color.copy(alpha = alpha), Offset(c * cell, y), Size(cell, height / segments * cell + cell))
        }
    }
}

private fun DrawScope.drawCelestial(pivot: Offset, t: Float, cell: Float, day: Float) {
    val rx = size.width * 0.44f
    val ry = size.height * 0.45f
    val a = (t - 0.25f) * 2f * PI.toFloat()
    val sun = Offset(snap(pivot.x - rx * cos(a), cell), snap(pivot.y - ry * sin(a), cell))
    val moon = Offset(snap(pivot.x + rx * cos(a), cell), snap(pivot.y + ry * sin(a), cell))

    if (sun.y < pivot.y) {
        drawCircle(
            Brush.radialGradient(listOf(Color(0x66FFD27A), Color.Transparent), center = sun, radius = cell * 34f),
            radius = cell * 34f, center = sun,
        )
        pixelDisc(sun, 6, cell, Color(0xFFFFC23D))
        pixelDisc(sun, 5, cell, Color(0xFFFFE680))
    }
    if (moon.y < pivot.y) {
        drawCircle(
            Brush.radialGradient(listOf(Color(0x40BFD0FF), Color.Transparent), center = moon, radius = cell * 24f),
            radius = cell * 24f, center = moon,
        )
        pixelDisc(moon, 5, cell, Color(0xFFE6EBFF))
        drawRect(Color(0xFFB8C2E8), Offset(moon.x - 2 * cell, moon.y - cell), Size(cell * 2, cell * 2))
        drawRect(Color(0xFFB8C2E8), Offset(moon.x + cell, moon.y + 2 * cell), Size(cell, cell))
        drawRect(Color(0xFFB8C2E8), Offset(moon.x + 2 * cell, moon.y - 3 * cell), Size(cell, cell))
    }
}

private fun DrawScope.pixelDisc(center: Offset, radius: Int, cell: Float, color: Color) {
    for (dy in -radius..radius) {
        val half = floor(sqrt((radius * radius - dy * dy).toFloat()) + 0.3f)
        drawRect(color, Offset(center.x - half * cell, center.y + dy * cell), Size((half * 2 + 1) * cell, cell))
    }
}

private fun DrawScope.drawClouds(clock: Float, cam: Float, cell: Float, rows: Float, day: Float) {
    val span = COLS + 80f
    val color = lerp(Color(0x552A3158), Color(0xE6FFFFFF), day)
    val shade = lerp(Color(0x40202644), Color(0xB3D6E6F7), day)
    for (i in 0 until 6) {
        val width = 16f + hash(i, 31) * 18f
        val x = ((hash(i, 32) * span + clock * (1.2f + hash(i, 33)) - cam * 0.08f).mod(span) - 40f)
        val y = floor(rows * (0.1f + hash(i, 34) * 0.24f))
        val left = floor(x) * cell
        val top = y * cell
        drawRect(shade, Offset(left, top + 2 * cell), Size(width * cell, cell))
        drawRect(color, Offset(left, top), Size(width * cell, 2 * cell))
        drawRect(color, Offset(left + 3 * cell, top - 2 * cell), Size((width - 8f) * cell, 2 * cell))
        drawRect(color, Offset(left + 6 * cell, top - 4 * cell), Size(floor(width * 0.4f) * cell, 2 * cell))
    }
}

// ---------------------------------------------------------------------------------------------
// Terrain

private class LayerSpec(val parallax: Float, val base: Float, val amp: Float, val haze: Float, val freq: Float)

private val LAYERS = listOf(
    LayerSpec(parallax = 0.12f, base = 0.40f, amp = 1f, haze = 0.55f, freq = 0.022f),
    LayerSpec(parallax = 0.28f, base = 0.31f, amp = 0.7f, haze = 0.36f, freq = 0.03f),
    LayerSpec(parallax = 0.55f, base = 0.22f, amp = 0.45f, haze = 0.17f, freq = 0.04f),
    LayerSpec(parallax = 1f, base = 0.13f, amp = 0.2f, haze = 0f, freq = 0.05f),
)

private class Palette(
    val layers: List<Color>,
    val top: Color,
    val dirt: Color,
    val speck: Color,
    val leaf: Color,
    val leafLight: Color,
    val trunk: Color,
)

private val PALETTES = mapOf(
    Biome.Forest to Palette(
        layers = listOf(Color(0xFF4F8F78), Color(0xFF3F7D50), Color(0xFF2F6B3A)),
        top = Color(0xFF52B848), dirt = Color(0xFF7A4E2D), speck = Color(0xFF5E5E6B),
        leaf = Color(0xFF3B9A38), leafLight = Color(0xFF66C24A), trunk = Color(0xFF6B4426),
    ),
    Biome.Autumn to Palette(
        layers = listOf(Color(0xFF8C6A62), Color(0xFF9E5A36), Color(0xFF7A4224)),
        top = Color(0xFFCB8530), dirt = Color(0xFF6A3F25), speck = Color(0xFF57505A),
        leaf = Color(0xFFE0682A), leafLight = Color(0xFFF2B33D), trunk = Color(0xFF5A3620),
    ),
    Biome.Snow to Palette(
        layers = listOf(Color(0xFF9DB2D2), Color(0xFF7690B6), Color(0xFF5B7598)),
        top = Color(0xFFF1F6FF), dirt = Color(0xFFA9BCD2), speck = Color(0xFF7A8CA6),
        leaf = Color(0xFF2E5C4F), leafLight = Color(0xFF3F7663), trunk = Color(0xFF4E3524),
    ),
    Biome.City to Palette(
        layers = listOf(Color(0xFF4C5274), Color(0xFF3A3F60), Color(0xFF2B2F4A)),
        top = Color(0xFF8A8C9C), dirt = Color(0xFF3C3C4A), speck = Color(0xFF52525F),
        leaf = Color(0xFFFFD873), leafLight = Color(0xFFFFF0B0), trunk = Color(0xFF2A2A35),
    ),
    Biome.Coast to Palette(
        layers = listOf(Color(0xFF6FA3B5), Color(0xFF2F7FC1), Color(0xFFD9BE86)),
        top = Color(0xFFF2DDA4), dirt = Color(0xFFD7B97C), speck = Color(0xFFBF9F66),
        leaf = Color(0xFF3FA35A), leafLight = Color(0xFF6CC56E), trunk = Color(0xFF9A6B3E),
    ),
)

private fun biomeAt(worldPos: Float): Int = floor(worldPos / BIOME_WIDTH).toInt().mod(Biome.entries.size)

/** How far a column has blended into the next biome (0 = fully its own). */
private fun blendAt(worldPos: Float): Float {
    val f = (worldPos / BIOME_WIDTH).mod(1f)
    val k = ((f - 0.72f) / 0.28f).coerceIn(0f, 1f)
    return k * k * (3 - 2 * k)
}

/** A terrain shape value (roughly -0.5..1.3) for one biome, layer, and layer column. */
private fun shape(biome: Int, layer: Int, x: Float, freq: Float): Float {
    val seed = layer * 101 + biome * 7
    return when (Biome.entries[biome]) {
        Biome.Forest -> (fbm(x * freq, seed) - 0.5f) * 0.9f + 0.1f
        Biome.Autumn -> (fbm(x * freq * 1.2f, seed) - 0.5f) * 1.1f + 0.15f
        Biome.Snow -> if (layer <= 1) {
            val ridge = 1f - abs(2f * noise(x * freq * 1.4f, seed) - 1f)
            ridge.pow(1.6f) * 1.4f - 0.05f
        } else {
            (fbm(x * freq, seed) - 0.5f) * 0.7f + 0.2f
        }
        Biome.City -> when (layer) {
            0 -> 0.15f + hash(floor(x / 6f).toInt(), seed) * 1.05f
            1 -> 0.05f + hash(floor(x / 8f).toInt(), seed) * 0.9f
            2 -> 0.1f + hash(floor(x / 11f).toInt(), seed) * 0.75f
            else -> 0f
        }
        Biome.Coast -> when (layer) {
            0 -> (fbm(x * freq, seed) - 0.5f) * 0.4f - 0.2f
            1 -> -0.45f
            2 -> (fbm(x * freq, seed) - 0.5f) * 0.4f - 0.55f
            else -> (fbm(x * freq, seed) - 0.5f) * 0.6f - 0.1f
        }
    }
}

private fun heightCells(layer: Int, spec: LayerSpec, c: Int, worldPos: Float, rows: Float): Float {
    val b = biomeAt(worldPos)
    val k = blendAt(worldPos)
    val s0 = shape(b, layer, c.toFloat(), spec.freq)
    val s = if (k > 0f) s0 + (shape((b + 1) % Biome.entries.size, layer, c.toFloat(), spec.freq) - s0) * k else s0
    return floor(rows * spec.base + s * rows * 0.2f * spec.amp)
}

private fun lit(c: Color, day: Float): Color {
    val dark = Color(
        (c.red * 0.22f + 0.02f).coerceIn(0f, 1f),
        (c.green * 0.26f + 0.03f).coerceIn(0f, 1f),
        (c.blue * 0.38f + 0.08f).coerceIn(0f, 1f),
    )
    return lerp(dark, c, day)
}

private fun DrawScope.drawLayer(
    index: Int,
    spec: LayerSpec,
    cam: Float,
    clock: Float,
    cell: Float,
    rows: Float,
    day: Float,
    sky: Color,
) {
    val layerCam = cam * spec.parallax
    val half = size.width / 2f / cell
    val first = floor(layerCam - half).toInt() - 1
    val last = floor(layerCam + half).toInt() + 1
    val biomes = Biome.entries
    val body = Array(biomes.size) { b ->
        val p = PALETTES.getValue(biomes[b])
        lerp(lit(p.layers.getOrElse(index) { p.dirt }, day), sky, spec.haze)
    }
    val front = index == LAYERS.lastIndex
    val night = 1f - day

    for (c in first..last) {
        val worldPos = cam + (c - layerCam)
        val x = (c - layerCam + half) * cell
        val b = biomeAt(worldPos)
        val k = blendAt(worldPos)
        val nb = (b + 1) % biomes.size
        val dominant = biomes[if (k < 0.5f) b else nb]
        val hc = heightCells(index, spec, c, worldPos, rows)
        val top = size.height - hc * cell
        val color = if (k > 0f) lerp(body[b], body[nb], k) else body[b]
        drawRect(color, Offset(x, top), Size(cell + 1f, size.height - top))

        val p = PALETTES.getValue(dominant)
        when {
            front -> {
                val grass = lit(p.top, day)
                drawRect(grass, Offset(x, top), Size(cell + 1f, cell * 2f))
                drawRect(lerp(grass, color, 0.5f), Offset(x, top + cell * 2f), Size(cell + 1f, cell))
                val speck = lit(p.speck, day)
                for (r in 4 until 40 step 2) {
                    if (hash(c, r) > 0.9f) drawRect(speck, Offset(x, top + r * cell), Size(cell + 1f, cell))
                }
                if (dominant == Biome.Forest && hash(c, 3) > 0.86f) {
                    val petal = listOf(Color(0xFFFF6B9A), Color(0xFFFFE45C), Color(0xFFB98CFF))[(hash(c, 4) * 3).toInt().coerceAtMost(2)]
                    drawRect(lit(Color(0xFF3B9A38), day), Offset(x, top - cell), Size(cell, cell))
                    drawRect(lit(petal, day), Offset(x, top - cell * 2), Size(cell, cell))
                }
            }
            dominant == Biome.Snow && index <= 1 -> {
                drawRect(lerp(lit(Color(0xFFF4F8FF), day), sky, spec.haze * 0.7f), Offset(x, top), Size(cell + 1f, cell * 3f))
            }
            dominant == Biome.Coast && index == 1 -> {
                // The open sea: shimmering highlights that slide along the surface.
                if (hash(c, floor(clock * 2f).toInt()) > 0.8f) {
                    drawRect(lerp(lit(Color(0xFFBFE6FF), day), sky, 0.2f), Offset(x, top), Size(cell + 1f, cell))
                }
            }
        }

        if (dominant == Biome.City && index < LAYERS.lastIndex && c.mod(3) == 1) {
            val window = lerp(Color(0xFFFFD873), sky, spec.haze * 0.6f)
            for (r in 2 until 60 step 3) {
                if (r > hc - 2) break
                val h = hash(c * 31 + r, index + 40)
                if (h > 0.5f) {
                    val flicker = if (h > 0.97f) (0.5f + 0.5f * sin(clock * 3f + c)) else 1f
                    val alpha = (night * 0.9f + 0.08f) * flicker * (0.5f + h * 0.5f)
                    drawRect(window.copy(alpha = alpha.coerceIn(0f, 1f)), Offset(x, top + r * cell), Size(cell, cell))
                }
            }
        }
    }

    // Decorations sit on the two nearest layers: silhouettes on the near hills, full color up front.
    if (index >= 2) {
        for (c in first - 12..last + 12) {
            if (c.mod(7) != 0) continue
            val worldPos = cam + (c - layerCam)
            val b = biomeAt(worldPos)
            val dominant = biomes[if (blendAt(worldPos) < 0.5f) b else (b + 1) % biomes.size]
            val chance = hash(c, 9 + index)
            if (chance > DENSITY.getValue(dominant)) continue
            val hc = heightCells(index, spec, c + 1, worldPos + 1, rows)
            val gx = (c - layerCam + half) * cell
            val gy = size.height - hc * cell
            val silhouette = if (front) null else lerp(body[dominant.ordinal], Color.Black, 0.15f)
            drawDecoration(dominant, c, gx, gy, cell, day, night, silhouette, front)
        }
    }
}

private val DENSITY = mapOf(
    Biome.Forest to 0.7f, Biome.Autumn to 0.75f, Biome.Snow to 0.6f, Biome.City to 0.45f, Biome.Coast to 0.35f,
)

private fun DrawScope.drawDecoration(
    biome: Biome,
    seed: Int,
    gx: Float,
    gy: Float,
    cell: Float,
    day: Float,
    night: Float,
    silhouette: Color?,
    front: Boolean,
) {
    val p = PALETTES.getValue(biome)
    fun col(c: Color) = silhouette ?: lit(c, day)
    fun r(dx: Int, dy: Int, w: Int, h: Int, c: Color) =
        drawRect(col(c), Offset(gx + dx * cell, gy + dy * cell), Size(w * cell + 0.5f, h * cell + 0.5f))

    val variety = hash(seed, 77)
    when (biome) {
        Biome.Forest, Biome.Autumn -> {
            if (front && biome == Biome.Forest) {
                val slot = seed.mod(56)
                if (slot == 0) {
                    drawHouse(gx, gy, cell, day, night)
                    return
                }
                if (slot <= 14 || slot >= 49) return // keep the yard around each house clear
            }
            if (variety < 0.25f) {
                r(-1, -2, 4, 2, p.leaf)
                r(0, -3, 2, 1, p.leafLight)
                if (front && biome == Biome.Autumn && variety < 0.08f) {
                    r(3, -2, 3, 2, Color(0xFFE8792A))
                    r(4, -3, 1, 1, Color(0xFF4E7A2A))
                }
                return
            }
            val h = 7 + (variety * 7).toInt()
            r(0, -h, 2, h, p.trunk)
            r(-1, -1, 4, 1, p.trunk)
            r(-2, -h / 2, 2, 1, p.trunk)
            r(-3, -h / 2 - 2, 2, 2, p.leaf)
            r(-2, -h - 5, 6, 1, p.leaf)
            r(-3, -h - 4, 8, 1, p.leaf)
            r(-4, -h - 3, 10, 3, p.leaf)
            r(-3, -h, 8, 1, p.leaf)
            r(-1, -h + 1, 4, 1, p.leaf)
            if (silhouette == null) {
                r(-2, -h - 4, 3, 1, p.leafLight)
                r(-3, -h - 3, 2, 1, p.leafLight)
                if (biome == Biome.Autumn) r(3, -h - 2, 2, 1, Color(0xFFD9452A))
            }
        }
        Biome.Snow -> {
            val h = 3 + (variety * 2).toInt()
            r(0, -3, 2, 3, p.trunk)
            for (tier in 0 until h) {
                val width = 10 - tier * 2
                val y = -3 - tier * 3
                r(1 - width / 2, y - 3, width, 3, p.leaf)
                if (silhouette == null) r(2 - width / 2, y - 3, width - 2, 1, Color(0xFFF4F8FF))
            }
            r(0, -3 - h * 3 - 1, 2, 1, if (silhouette == null) Color(0xFFF4F8FF) else p.leaf)
        }
        Biome.City -> {
            if (!front) return
            r(0, -10, 1, 10, Color(0xFF2E2E3A))
            r(-1, -11, 3, 1, Color(0xFF2E2E3A))
            val bulb = lerp(Color(0xFF6B6650), Color(0xFFFFE08A), night)
            drawRect(bulb, Offset(gx - cell, gy - 10 * cell), Size(3 * cell, cell))
            if (night > 0.1f) {
                val c = Offset(gx + cell / 2, gy - 9 * cell)
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFFFFD873).copy(alpha = 0.35f * night), Color.Transparent), center = c, radius = 12 * cell),
                    radius = 12 * cell, center = c,
                )
            }
            if (variety > 0.6f) {
                r(4, -2, 5, 1, Color(0xFF6B4A33))
                r(4, -1, 1, 1, Color(0xFF2E2E3A))
                r(8, -1, 1, 1, Color(0xFF2E2E3A))
            }
        }
        Biome.Coast -> {
            if (!front) return
            r(0, -2, 2, 2, p.trunk)
            r(1, -5, 2, 3, p.trunk)
            r(2, -8, 2, 3, p.trunk)
            r(2, -10, 2, 2, p.trunk)
            r(-2, -11, 5, 1, p.leaf)
            r(4, -11, 5, 1, p.leaf)
            r(-3, -10, 2, 1, p.leaf)
            r(8, -10, 2, 1, p.leaf)
            r(1, -12, 4, 1, p.leafLight)
            r(-4, -9, 1, 1, p.leaf)
            r(9, -9, 1, 1, p.leaf)
            r(2, -10, 1, 1, Color(0xFF6A4A2A))
        }
    }
}

private fun DrawScope.drawHouse(gx: Float, gy: Float, cell: Float, day: Float, night: Float) {
    fun r(dx: Int, dy: Int, w: Int, h: Int, c: Color) =
        drawRect(c, Offset(gx + dx * cell, gy + dy * cell), Size(w * cell + 0.5f, h * cell + 0.5f))
    val wall = lit(Color(0xFFA0703F), day)
    val beam = lit(Color(0xFF6E4524), day)
    val roof = lit(Color(0xFF9A3B2E), day)
    r(0, -7, 13, 7, wall)
    r(0, -7, 1, 7, beam)
    r(12, -7, 1, 7, beam)
    r(9, -13, 2, 4, lit(Color(0xFF6B6B78), day))
    for (i in 0 until 5) r(-1 + i, -8 - i, 15 - i * 2, 1, roof)
    r(2, -5, 3, 5, lit(Color(0xFF4A2C17), day))
    val glow = lerp(lit(Color(0xFF2B3A55), day), Color(0xFFFFD873), night)
    r(7, -5, 3, 2, glow)
    if (night > 0.2f) {
        val c = Offset(gx + 8.5f * cell, gy - 4 * cell)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFFFC85A).copy(alpha = 0.3f * night), Color.Transparent), center = c, radius = 9 * cell),
            radius = 9 * cell, center = c,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Weather

private fun DrawScope.drawFireflies(clock: Float, cam: Float, cell: Float, amount: Float) {
    if (amount < 0.02f) return
    val span = size.width + 20 * cell
    for (i in 0 until 26) {
        val x = (hash(i, 61) * span + sin(clock * 0.4f + i) * 8 * cell - cam * cell * 0.8f).mod(span) - 10 * cell
        val y = size.height * (0.6f + hash(i, 62) * 0.3f) + cos(clock * 0.5f + i * 1.7f) * 5 * cell
        val alpha = amount * (0.5f + 0.5f * sin(clock * 3f + i * 2.1f))
        val c = Offset(snap(x, cell), snap(y, cell))
        drawCircle(Color(0xFFD8FF6A).copy(alpha = alpha * 0.22f), radius = 3 * cell, center = c + Offset(cell / 2, cell / 2))
        drawRect(Color(0xFFEFFFA8).copy(alpha = alpha), c, Size(cell, cell))
    }
}

private fun DrawScope.drawLeaves(clock: Float, cam: Float, cell: Float, amount: Float, day: Float) {
    val spanX = size.width + 20 * cell
    val spanY = size.height + 10 * cell
    val colors = listOf(Color(0xFFE0682A), Color(0xFFF2B33D), Color(0xFFD9452A))
    for (i in 0 until 44) {
        val x = (hash(i, 41) * spanX - clock * (10f + 10f * hash(i, 42)) * cell + sin(clock * 1.3f + i) * 3 * cell - cam * cell * 0.9f)
            .mod(spanX) - 10 * cell
        val y = (hash(i, 43) * spanY + clock * (14f + 10f * hash(i, 44)) * cell).mod(spanY) - 5 * cell
        val c = lit(colors[i % colors.size], 0.35f + day * 0.65f).copy(alpha = amount)
        val flip = sin(clock * 4f + i) > 0f
        drawRect(c, Offset(snap(x, cell), snap(y, cell)), Size(if (flip) 2 * cell else cell, if (flip) cell else 2 * cell))
    }
}

private fun DrawScope.drawSnow(clock: Float, cam: Float, cell: Float, amount: Float) {
    val spanX = size.width + 20 * cell
    val spanY = size.height + 10 * cell
    for (i in 0 until 110) {
        val depth = 0.4f + hash(i, 51) * 0.6f
        val x = (hash(i, 52) * spanX + sin(clock * 0.8f + i) * 4 * cell - cam * cell * depth).mod(spanX) - 10 * cell
        val y = (hash(i, 53) * spanY + clock * (5f + 9f * depth) * cell).mod(spanY) - 5 * cell
        drawRect(Color.White.copy(alpha = amount * (0.4f + depth * 0.6f)), Offset(snap(x, cell), snap(y, cell)), Size(cell, cell))
    }
}

// ---------------------------------------------------------------------------------------------
// Noise

private fun snap(v: Float, cell: Float) = floor(v / cell) * cell

private fun hash(a: Int, b: Int = 0): Float {
    var x = a * 374761393 + b * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0x7fffffff) / 2147483647f
}

private fun noise(x: Float, seed: Int): Float {
    val i = floor(x).toInt()
    val f = x - i
    val s = f * f * (3 - 2 * f)
    val a = hash(i, seed)
    return a + (hash(i + 1, seed) - a) * s
}

private fun fbm(x: Float, seed: Int) = noise(x, seed) * 0.65f + noise(x * 2.3f, seed + 17) * 0.35f
