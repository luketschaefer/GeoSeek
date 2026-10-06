// Shared building blocks: outlined text, the rocking logo, pixel panels, menu items, buttons, fields, and bars.
package com.example.geoseek.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Text

/** White text with a dark outline, readable over any sky. */
@Composable
fun OutlinedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = GeoColors.Text,
    outline: Color = GeoColors.Outline,
    outlineWidth: Dp = 3.dp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val stroke = with(LocalDensity.current) { outlineWidth.toPx() }
    Box(modifier) {
        Text(
            text,
            style = style.copy(drawStyle = Stroke(stroke, join = StrokeJoin.Round), textAlign = textAlign ?: style.textAlign),
            color = outline,
            maxLines = maxLines,
        )
        Text(text, style = style.copy(textAlign = textAlign ?: style.textAlign), color = color, maxLines = maxLines)
    }
}

/** The title-screen logo: grass-to-earth letters that slowly rock and breathe. */
@Composable
fun GeoLogo(modifier: Modifier = Modifier, fontSize: Float = 56f, rocking: Boolean = true) {
    val motion = rememberInfiniteTransition(label = "logo")
    val tilt by motion.animateFloat(-2.5f, 2.5f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilt")
    val breathe by motion.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val style = GeoType.Logo.copy(fontSize = fontSize.sp)
    val density = LocalDensity.current
    val stroke = with(density) { (fontSize / 7f).dp.toPx() }
    val shadow = with(density) { (fontSize / 16f).dp.toPx() }

    Box(
        modifier.graphicsLayer {
            if (rocking) {
                rotationZ = tilt
                scaleX = breathe
                scaleY = breathe
            }
        },
    ) {
        Text("GeoSeek", style = style.copy(drawStyle = Stroke(stroke, join = StrokeJoin.Round)), color = Color(0x99000000), modifier = Modifier.graphicsLayer { translationY = shadow })
        Text("GeoSeek", style = style.copy(drawStyle = Stroke(stroke, join = StrokeJoin.Round)), color = Color(0xFF1B140C))
        Text(
            "GeoSeek",
            style = style.copy(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFC6F57A),
                    0.38f to Color(0xFF5DC23E),
                    0.42f to Color(0xFF9A6A3C),
                    1f to Color(0xFF5A3A1F),
                ),
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Panels

/** A rectangle with stair-stepped pixel corners. */
class PixelShape(private val step: Dp = 4.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { step.toPx() }
        return Outline.Generic(pixelPath(0f, 0f, size.width, size.height, s))
    }
}

fun pixelPath(l: Float, t: Float, r: Float, b: Float, s: Float): Path = Path().apply {
    moveTo(l + 2 * s, t)
    lineTo(r - 2 * s, t); lineTo(r - 2 * s, t + s); lineTo(r - s, t + s); lineTo(r - s, t + 2 * s); lineTo(r, t + 2 * s)
    lineTo(r, b - 2 * s); lineTo(r - s, b - 2 * s); lineTo(r - s, b - s); lineTo(r - 2 * s, b - s); lineTo(r - 2 * s, b)
    lineTo(l + 2 * s, b); lineTo(l + 2 * s, b - s); lineTo(l + s, b - s); lineTo(l + s, b - 2 * s); lineTo(l, b - 2 * s)
    lineTo(l, t + 2 * s); lineTo(l + s, t + 2 * s); lineTo(l + s, t + s); lineTo(l + 2 * s, t + s)
    close()
}

/** The game's panel look: translucent fill, a dark outer edge, and a lighter inner border. */
fun Modifier.pixelPanel(
    fill: Color = GeoColors.Panel,
    border: Color = GeoColors.PanelBorder,
    step: Dp = 4.dp,
): Modifier = drawBehind {
    val s = step.toPx()
    val e = 2.dp.toPx()
    drawPath(pixelPath(0f, 0f, size.width, size.height, s), fill)
    drawPath(pixelPath(0f, 0f, size.width, size.height, s), GeoColors.PanelEdge, style = Stroke(e * 2))
    drawPath(pixelPath(e * 1.5f, e * 1.5f, size.width - e * 1.5f, size.height - e * 1.5f, s * 0.75f), border, style = Stroke(e))
    // A one-pixel shine along the top, like light catching the frame.
    drawRect(Color.White.copy(alpha = 0.10f), Offset(2 * s, e * 2.5f), Size(size.width - 4 * s, e))
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    fill: Color = GeoColors.Panel,
    border: Color = GeoColors.PanelBorder,
    content: @Composable () -> Unit,
) {
    Box(modifier.pixelPanel(fill, border).padding(18.dp)) { content() }
}

/** Fades and lifts content in a beat after it appears, so lists cascade in. */
@Composable
fun Modifier.entrance(index: Int, baseDelay: Int = 60): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * baseDelay.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

// ---------------------------------------------------------------------------------------------
// Buttons

/** A big title-screen menu entry: outlined text that grows and turns yellow when pressed. */
@Composable
fun MenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Sprite? = null,
    accent: Color = GeoColors.Highlight,
    caption: String? = null,
    style: TextStyle = GeoType.Menu,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 1.12f else 1f, spring(0.45f, Spring.StiffnessMedium), label = "menuScale")
    val color by animateColorAsState(if (pressed) GeoColors.Highlight else GeoColors.Text, tween(120), label = "menuColor")

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PixelSprite(icon, Modifier.size(30.dp), tint = accent)
            Spacer(Modifier.width(14.dp))
        }
        Column {
            OutlinedText(text, style, color = color)
            if (caption != null) OutlinedText(caption, GeoType.Caption, color = GeoColors.TextSoft, outlineWidth = 2.dp)
        }
    }
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Scenes.Title.accent,
    enabled: Boolean = true,
    icon: Sprite? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.5f, Spring.StiffnessMedium), label = "btnScale")
    val fill = when {
        !enabled -> Color(0x99343A5C)
        pressed -> lerpColor(color, Color.White, 0.15f)
        else -> lerpColor(color, Color.Black, 0.35f).copy(alpha = 0.92f)
    }
    val textColor by animateColorAsState(if (pressed) GeoColors.Highlight else GeoColors.Text, tween(100), label = "btnText")

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.6f }
            .pixelPanel(fill, if (enabled) color else GeoColors.TextMuted)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PixelSprite(icon, Modifier.size(18.dp), tint = textColor)
            Spacer(Modifier.width(10.dp))
        }
        OutlinedText(text, GeoType.Heading, color = textColor, outlineWidth = 2.5.dp)
    }
}

/** Small "back to menu" control used at the top of every mode screen. */
@Composable
fun BackToMenu(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "Menu") {
    MenuItem(label, onClick, modifier, icon = Icons.Back, accent = GeoColors.TextSoft, style = GeoType.Heading)
}

/** The header every mode screen shares: back control, a big outlined title, and a subtitle. */
@Composable
fun ScreenHeader(title: String, subtitle: String, accent: Color, onBack: () -> Unit, icon: Sprite? = null) {
    Column(Modifier.fillMaxWidth()) {
        BackToMenu(onBack, Modifier.padding(start = 0.dp))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                PixelSprite(icon, Modifier.size(34.dp), tint = accent)
                Spacer(Modifier.width(14.dp))
            }
            OutlinedText(title, GeoType.Display, outlineWidth = 4.dp)
        }
        OutlinedText(subtitle, GeoType.Body, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = GeoColors.TextSoft, outlineWidth = 2.5.dp)
    }
}

// ---------------------------------------------------------------------------------------------
// Inputs and meters

@Composable
fun GeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    error: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        when {
            error -> GeoColors.Danger
            focused -> GeoColors.Highlight
            else -> GeoColors.SlotBorder
        },
        tween(200), label = "fieldBorder",
    )

    Column(modifier) {
        OutlinedText(label, GeoType.Label, color = if (error) GeoColors.Danger else GeoColors.TextSoft, outlineWidth = 2.dp)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = GeoType.Body.copy(color = GeoColors.Text, fontSize = 16.sp),
            cursorBrush = SolidColor(GeoColors.Highlight),
            visualTransformation = if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Ascii,
                imeAction = imeAction,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }, onGo = { onImeAction() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pixelPanel(GeoColors.PanelDeep, border, step = 3.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { inner() }
                    if (password) {
                        Text(
                            if (reveal) "HIDE" else "SHOW",
                            style = GeoType.Caption.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                            color = GeoColors.TextMuted,
                            modifier = Modifier
                                .clickable(remember { MutableInteractionSource() }, indication = null) { reveal = !reveal }
                                .padding(start = 12.dp),
                        )
                    }
                }
            },
        )
    }
}

/** A segmented pixel bar, like a health or mana meter. */
@Composable
fun PixelBar(progress: Float, color: Color, modifier: Modifier = Modifier, segments: Int = 20) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(0.8f, Spring.StiffnessLow), label = "bar")
    Canvas(modifier.height(14.dp)) {
        val e = 2.dp.toPx()
        drawRect(GeoColors.PanelEdge)
        drawRect(Color(0xFF1A1F45), Offset(e, e), Size(size.width - 2 * e, size.height - 2 * e))
        val gap = 1.5.dp.toPx()
        val inner = size.width - 2 * e
        val segW = (inner - gap * (segments - 1)) / segments
        val filled = animated * segments
        for (i in 0 until segments) {
            val f = (filled - i).coerceIn(0f, 1f)
            if (f <= 0f) break
            val x = e + i * (segW + gap)
            drawRect(color, Offset(x, e), Size(segW * f, size.height - 2 * e))
            drawRect(Color.White.copy(alpha = 0.35f), Offset(x, e), Size(segW * f, 2.dp.toPx()))
        }
    }
}

/** A square inventory-style slot. */
fun Modifier.slot(border: Color = GeoColors.SlotBorder, fill: Color = GeoColors.Slot): Modifier =
    pixelPanel(fill, border, step = 3.dp)

@Composable
fun StatSlot(label: String, value: String, icon: Sprite, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.slot().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSprite(icon, Modifier.size(16.dp), tint = accent)
            Spacer(Modifier.width(8.dp))
            Text(label.uppercase(), style = GeoType.Caption.copy(fontSize = 11.sp, letterSpacing = 1.sp), color = GeoColors.TextSoft)
        }
        Spacer(Modifier.height(6.dp))
        OutlinedText(value, GeoType.Title, outlineWidth = 2.5.dp)
    }
}

fun lerpColor(a: Color, b: Color, f: Float): Color = androidx.compose.ui.graphics.lerp(a, b, f)
