package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.ui.theme.CustomThemePalette
import java.io.File
import java.io.FileOutputStream

object ReceiptImageHelper {

    fun createCalculationBitmap(
        expression: String,
        result: String,
        dateString: String,
        southAsianWords: String?,
        internationalWords: String?,
        palette: CustomThemePalette
    ): Bitmap {
        val width = 1080
        val padding = 64f
        val innerPadding = 52f
        val contentWidth = (width - (padding * 2 + innerPadding * 2)).toInt()

        val exprPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryText.toArgb()
            textSize = 44f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val exprLayout = StaticLayout.Builder
            .obtain("$expression =", 0, expression.length + 2, exprPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(8f, 1.0f)
            .build()

        val resultSize = when {
            result.length > 18 -> 52f
            result.length > 12 -> 68f
            else -> 88f
        }
        val resultPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent.toArgb()
            textSize = resultSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val resultLayout = StaticLayout.Builder
            .obtain(result, 0, result.length, resultPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setLineSpacing(8f, 1.0f)
            .build()

        val wordsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mainText.toArgb()
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val wordsText = buildString {
            if (!southAsianWords.isNullOrBlank()) append("In Words: $southAsianWords")
            if (!internationalWords.isNullOrBlank()) {
                if (isNotEmpty()) append("\n")
                append("International: $internationalWords")
            }
        }
        val wordsLayout = if (wordsText.isNotEmpty()) {
            StaticLayout.Builder
                .obtain(wordsText, 0, wordsText.length, wordsPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(10f, 1.0f)
                .build()
        } else null

        val headerHeight = 140f
        val footerHeight = 90f
        val dividerSpacing = 48f
        val cardContentHeight = headerHeight +
            exprLayout.height + 28f +
            resultLayout.height + 36f +
            (if (wordsLayout != null) dividerSpacing + wordsLayout.height else 0f) +
            footerHeight

        val height = (padding * 2 + cardContentHeight).toInt().coerceAtLeast(620)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer background
        canvas.drawColor(palette.mainBg.toArgb())

        // Card surface
        val cardRect = RectF(padding, padding, width - padding, height - padding)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryBg.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.border.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, borderPaint)

        var y = padding + innerPadding
        val leftX = padding + innerPadding
        val rightX = width - padding - innerPadding

        // Brand badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primaryOp.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(leftX, y, leftX + 64f, y + 64f), 16f, 16f, badgePaint)
        val dPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent.toArgb()
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("D", leftX + 32f, y + 45f, dPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mainText.toArgb()
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("D. CALC SLIP", leftX + 86f, y + 32f, titlePaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mutedText.toArgb()
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(dateString, leftX + 86f, y + 64f, datePaint)

        y += 106f

        // Top divider
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.border.toArgb()
            strokeWidth = 2f
        }
        canvas.drawLine(leftX, y, rightX, y, divPaint)
        y += 36f

        // Expression
        canvas.save()
        canvas.translate(leftX, y)
        exprLayout.draw(canvas)
        canvas.restore()
        y += exprLayout.height + 28f

        // Result
        canvas.save()
        canvas.translate(leftX, y)
        resultLayout.draw(canvas)
        canvas.restore()
        y += resultLayout.height + 36f

        // Words
        if (wordsLayout != null) {
            canvas.drawLine(leftX, y, rightX, y, divPaint)
            y += 28f
            canvas.save()
            canvas.translate(leftX, y)
            wordsLayout.draw(canvas)
            canvas.restore()
        }

        // Footer watermark
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mutedText.toArgb()
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Generated by D. Calc", rightX, height - padding - 28f, footerPaint)

        return bitmap
    }

    fun saveScreenshotToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val fileName = "D_Calc_Slip_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DCalc")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    Toast.makeText(context, "Screenshot saved to Pictures/DCalc", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    false
                }
            } else {
                // On pre-Q without legacy write permission, save to app cache and offer share/save
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "DCalc")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, "Screenshot saved: ${file.name}", Toast.LENGTH_SHORT).show()
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Screenshot failed: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareReceiptImage(context: Context, bitmap: Bitmap) {
        try {
            val shareDir = File(context.cacheDir, "shares")
            if (!shareDir.exists()) shareDir.mkdirs()
            val file = File(shareDir, "d_calc_slip_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Calculation Screenshot"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
