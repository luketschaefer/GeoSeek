// Pixel-art sprites: menu icons, a sprite per catalog object, and the player's little explorer.
// Each sprite is a grid of characters; '.' is empty and every other character maps to a palette color.
package com.example.geoseek.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.geoseek.models.GameObject
import kotlin.math.floor

class Sprite(val rows: List<String>, val palette: Map<Char, Color> = emptyMap()) {
    val width = rows.maxOf { it.length }
    val height = rows.size
}

/** Draws [sprite] centered and pixel-snapped. 'X' pixels take [tint]; [shadow] adds a dark drop edge. */
@Composable
fun PixelSprite(sprite: Sprite, modifier: Modifier = Modifier, tint: Color = Color.White, shadow: Boolean = true, alpha: Float = 1f) {
    Canvas(modifier) { drawSprite(sprite, tint, shadow, alpha) }
}

fun DrawScope.drawSprite(sprite: Sprite, tint: Color = Color.White, shadow: Boolean = true, alpha: Float = 1f) {
    val pad = if (shadow) 1 else 0
    val cell = floor(minOf(size.width / (sprite.width + pad), size.height / (sprite.height + pad))).coerceAtLeast(1f)
    val left = floor((size.width - cell * (sprite.width + pad)) / 2f)
    val top = floor((size.height - cell * (sprite.height + pad)) / 2f)
    val shadowColor = GeoColors.Outline.copy(alpha = 0.7f * alpha)
    if (shadow) {
        sprite.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch != '.') drawRect(shadowColor, Offset(left + (x + 1) * cell, top + (y + 1) * cell), Size(cell, cell))
            }
        }
    }
    sprite.rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            if (ch == '.') return@forEachIndexed
            val c = if (ch == 'X') tint else sprite.palette[ch] ?: tint
            drawRect(c.copy(alpha = c.alpha * alpha), Offset(left + x * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

object Icons {
    val Hunt = Sprite(
        listOf(
            "...XXXXX...",
            "..X..X..X..",
            ".X...X...X.",
            "X....X....X",
            "X.........X",
            "XXXX.X.XXXX",
            "X.........X",
            "X....X....X",
            ".X...X...X.",
            "..X..X..X..",
            "...XXXXX...",
        ),
    )
    val Explore = Sprite(
        listOf(
            "....XXXXX....",
            "..XX.....XX..",
            ".X...XXX...X.",
            "X...XX.XX...X",
            "X...X.W.X...X",
            "X...XX.XX...X",
            ".X...XXX...X.",
            "..XX.....XX..",
            "....XXXXX....",
        ),
        mapOf('W' to Color.White),
    )
    val Cards = Sprite(
        listOf(
            "...XXXXXXX",
            "...X.....X",
            "XXXXXXX..X",
            "X.....X..X",
            "X..X..X..X",
            "X.XXX.X..X",
            "X..X..X..X",
            "X.....XXXX",
            "X.....X...",
            "XXXXXXX...",
        ),
    )
    val Profile = Sprite(
        listOf(
            "...XXXX...",
            "..XXXXXX..",
            "..XXXXXX..",
            "..XXXXXX..",
            "...XXXX...",
            "..........",
            ".XXXXXXXX.",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
        ),
    )
    val Back = Sprite(
        listOf(
            "...X....",
            "..XX....",
            ".XXXXXXX",
            "XXXXXXXX",
            ".XXXXXXX",
            "..XX....",
            "...X....",
        ),
    )
    val Star = Sprite(
        listOf(
            "....X....",
            "....X....",
            "...XXX...",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "..XXXXX..",
            "..XX.XX..",
            ".XX...XX.",
            ".X.....X.",
        ),
    )
    val Clock = Sprite(
        listOf(
            "..XXXXX..",
            ".X..X..X.",
            "X...X...X",
            "X...X...X",
            "X...XXX.X",
            "X.......X",
            "X.......X",
            ".X.....X.",
            "..XXXXX..",
        ),
    )
    val Trophy = Sprite(
        listOf(
            "XXXXXXXXX",
            "X.XXXXX.X",
            "X.XXXXX.X",
            ".XXXXXXX.",
            "..XXXXX..",
            "...XXX...",
            "....X....",
            "..XXXXX..",
            "..XXXXX..",
        ),
    )
    val Check = Sprite(
        listOf(
            "........X",
            ".......XX",
            "......XX.",
            "X....XX..",
            "XX..XX...",
            ".XXXX....",
            "..XX.....",
        ),
    )
    val Question = Sprite(
        listOf(
            ".XXXXX.",
            "XX...XX",
            ".....XX",
            "...XXX.",
            "..XX...",
            ".......",
            "..XX...",
            "..XX...",
        ),
    )
    val Door = Sprite(
        listOf(
            "XXXXXX...",
            "X....X...",
            "X....X.X.",
            "X..X.XXXX",
            "X....X.X.",
            "X....X...",
            "XXXXXX...",
        ),
    )
    val Leaf = Sprite(
        listOf(
            "......XXX",
            "....XXXXX",
            "..XXXXXX.",
            ".XXX.XXX.",
            ".XX.XXX..",
            "X..XX....",
            "X........",
        ),
    )
}

object ObjectSprites {
    private val Chair = Sprite(
        listOf(
            "..dbbbbbbd..",
            "..dbllllbd..",
            "..db....bd..",
            "..dbllllbd..",
            "..db....bd..",
            "..dbbbbbbd..",
            ".dllllllllb.",
            ".dbbbbbbbbd.",
            "..d......d..",
            "..d......d..",
            "..d......d..",
            "..d......d..",
        ),
        mapOf('b' to Color(0xFF9A6338), 'd' to Color(0xFF5E3A1E), 'l' to Color(0xFFC88E55)),
    )
    private val Plant = Sprite(
        listOf(
            ".....g......",
            "...g.gG..g..",
            "..gGgGg.gG..",
            "...GgGgGg...",
            ".g..GgG..g..",
            ".gG.gGg.gG..",
            "..GgGgGgG...",
            "....GgG.....",
            "..pppppppp..",
            "..pPPPPPPp..",
            "...pPPPPp...",
            "...pppppp...",
        ),
        mapOf('g' to Color(0xFF6CCB4E), 'G' to Color(0xFF2F8A3A), 'p' to Color(0xFFD27148), 'P' to Color(0xFF9A4A2A)),
    )
    private val Bicycle = Sprite(
        listOf(
            "..........kk..",
            "..sss......r..",
            "....r......r..",
            "....rrrrrrrr..",
            ".kkkr....r.kk.",
            "k...rr..r.r..k",
            "k.o..rrr...o.k",
            "k.........r..k",
            ".kkk......kkk.",
        ),
        mapOf('k' to Color(0xFF2B2B36), 'r' to Color(0xFFE5483C), 's' to Color(0xFF6B4A33), 'o' to Color(0xFF9AA0B5)),
    )
    private val Balloon = Sprite(
        listOf(
            "...rryyrr...",
            "..rryyyyrr..",
            ".rrryyyyrrr.",
            ".rrryyyyrrr.",
            "rrrryyyyrrrr",
            "rrrryyyyrrrr",
            ".rrryyyyrrr.",
            ".rrryyyyrrr.",
            "..rryyyyrr..",
            "...rryyrr...",
            "....l..l....",
            "....l..l....",
            "....wwww....",
            "....wWWw....",
        ),
        mapOf('r' to Color(0xFFE8473B), 'y' to Color(0xFFFFD34D), 'l' to Color(0xFFC9B48A), 'w' to Color(0xFF8A5A33), 'W' to Color(0xFF6A4224)),
    )
    private val Telescope = Sprite(
        listOf(
            "..........cc",
            "........nnnc",
            "......nnnnn.",
            "....nnbnn...",
            "..nnnnn.....",
            "nnnnn.......",
            "nn...k......",
            ".....k......",
            "....k.k.....",
            "...k...k....",
            "..k.....k...",
            ".k.......k..",
        ),
        mapOf('n' to Color(0xFF3550A8), 'c' to Color(0xFFA9C8FF), 'b' to Color(0xFFFFC94D), 'k' to Color(0xFF6B4426)),
    )
    private val Gem = Sprite(
        listOf(
            "...XXXX...",
            "..XWXXXX..",
            ".XWXXXXXX.",
            "XXXXXXXXXX",
            ".XXXXXXXX.",
            "..XXXXXX..",
            "...XXXX...",
            "....XX....",
        ),
        mapOf('W' to Color.White),
    )

    /** The sprite for a catalog object, or a gem tinted by rarity for objects without art yet. */
    fun forObject(obj: GameObject): Sprite = when (obj.name.lowercase()) {
        "chair" -> Chair
        "plant" -> Plant
        "bicycle" -> Bicycle
        "hot air balloon" -> Balloon
        "telescope" -> Telescope
        else -> Gem
    }
}

/** A little explorer whose hair, shirt, and pants are picked from their name. */
fun explorerSprite(name: String): Sprite {
    val seed = name.lowercase().hashCode()
    fun pick(colors: List<Color>, shift: Int) = colors[Math.floorMod(seed shr shift, colors.size)]
    val hair = pick(listOf(Color(0xFF3B2416), Color(0xFFE0B04A), Color(0xFFB0472E), Color(0xFF1E1E28), Color(0xFF8E5BD8), Color(0xFF3FA39A)), 0)
    val skin = pick(listOf(Color(0xFFF2C6A0), Color(0xFFD9A273), Color(0xFFA86F48), Color(0xFF6E4430)), 4)
    val shirt = pick(listOf(Color(0xFF3F7DE0), Color(0xFF3BA55C), Color(0xFFE0553F), Color(0xFFE0A93F), Color(0xFF8D5CE0)), 8)
    val pants = pick(listOf(Color(0xFF2E3A66), Color(0xFF5A4030), Color(0xFF3A3A44), Color(0xFF29524A)), 12)
    return Sprite(
        listOf(
            "...hhhh...",
            "..hhhhhh..",
            ".hhsssss..",
            ".hhsessE..",
            "..ssssss..",
            "...ssss...",
            "..tttttt..",
            ".sttttttS.",
            ".s.tttt.s.",
            "...pppp...",
            "...p..p...",
            "..bb..bb..",
        ),
        mapOf(
            'h' to hair, 's' to skin, 'S' to skin, 'e' to Color(0xFF1A1A2E), 'E' to Color(0xFF1A1A2E),
            't' to shirt, 'p' to pants, 'b' to Color(0xFF4A2C17),
        ),
    )
}
