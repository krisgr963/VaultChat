package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkSurface
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Deterministic visual QR / Matrix representation of a cryptographic public key
 * for in-person zero-trust verification (Safety Number verification).
 */
@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = CyberCyan,
    backgroundColor: Color = CyberDarkSurface
) {
    val matrixSize = 21 // Standard 21x21 QR Version 1 dimensions
    val grid = remember(data) {
        val digest = MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8))
        val array = Array(matrixSize) { BooleanArray(matrixSize) }

        // Fill corner finder patterns
        drawFinderPattern(array, 0, 0)
        drawFinderPattern(array, matrixSize - 7, 0)
        drawFinderPattern(array, 0, matrixSize - 7)

        // Fill timing patterns
        for (i in 7 until matrixSize - 7) {
            array[6][i] = (i % 2 == 0)
            array[i][6] = (i % 2 == 0)
        }

        // Fill data bits deterministically from hash
        var bitIndex = 0
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (isReservedPattern(r, c, matrixSize)) continue
                val byteVal = digest[bitIndex % digest.size].toInt()
                val bit = ((byteVal shr (bitIndex % 8)) and 1) == 1
                array[r][c] = bit
                bitIndex++
            }
        }
        array
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / matrixSize
            val cellHeight = size.height / matrixSize

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (grid[r][c]) {
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth * 0.95f, cellHeight * 0.95f)
                        )
                    }
                }
            }
        }
    }
}

private fun drawFinderPattern(grid: Array<BooleanArray>, startR: Int, startC: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val isBorder = r == 0 || r == 6 || c == 0 || c == 6
            val isCenter = r in 2..4 && c in 2..4
            grid[startR + r][startC + c] = isBorder || isCenter
        }
    }
}

private fun isReservedPattern(r: Int, c: Int, size: Int): Boolean {
    // Top-left finder
    if (r < 8 && c < 8) return true
    // Top-right finder
    if (r < 8 && c >= size - 8) return true
    // Bottom-left finder
    if (r >= size - 8 && c < 8) return true
    // Timing pattern
    if (r == 6 || c == 6) return true
    return false
}
