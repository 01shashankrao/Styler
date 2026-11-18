package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StylerConstants
import com.example.util.ImageCompressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read Styler app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Styler", appName)
    }

    @Test
    fun `verify body frame calculation and 15 wearable and perfume categories`() {
        assertEquals(15, StylerConstants.WARDROBE_CATEGORIES.size)
        assertTrue(StylerConstants.WARDROBE_CATEGORIES.contains("Jacket"))
        assertTrue(StylerConstants.WARDROBE_CATEGORIES.contains("Hoodie"))
        assertTrue(StylerConstants.WARDROBE_CATEGORIES.contains("Pullover"))
        assertTrue(StylerConstants.WARDROBE_CATEGORIES.contains("Sweatshirt"))
        assertTrue(StylerConstants.WARDROBE_CATEGORIES.contains("Perfume"))
        val frame = StylerConstants.calculateBodyFrame(178, 74)
        assertEquals("Athletic", frame.shortLabel)
        assertTrue(frame.tag.contains("Athletic"))
    }

    @Test
    fun `verify canvas image compression produces valid base64`() {
        val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.parseColor("#264A31"))
        val base64 = ImageCompressor.compressBitmap(bitmap)
        assertTrue(base64.isNotBlank())
        val decoded = ImageCompressor.decodeBase64ToBitmap(base64)
        assertNotNull(decoded)
        assertTrue(decoded!!.width <= 600)
    }
}
