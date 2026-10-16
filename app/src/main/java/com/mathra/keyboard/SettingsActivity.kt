package com.mathra.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.exp

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private val previews = ArrayList<ThemePreviewView>()

    private val gold = 0xFFFFC83D.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val soft = 0xFFC9BCE8.toInt()

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF120B2B.toInt(), 0xFF2A0F4A.toInt())
            )
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(18), px(24), px(18), px(40))
        }
        root.addView(col)

        // ---- header ----
        col.addView(ImageView(this).apply {
            setImageResource(R.drawable.mathra_logo)
            adjustViewBounds = true
        }, LinearLayout.LayoutParams(px(150), px(150)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        col.addView(label("MATHRA KEYBOARD", 26f, gold, true).apply { gravity = Gravity.CENTER })
        col.addView(label("Premium 3D Teddy Typing", 14f, soft, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, px(2), 0, px(16))
        })

        // ---- setup ----
        val setup = card(col, "Get started")
        status = label("", 14f, white, false).apply { setPadding(0, px(6), 0, px(10)) }
        setup.addView(status)
        setup.addView(button("1  Enable MATHRA Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        setup.addView(button("2  Switch to MATHRA Keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })

        // ---- themes ----
        val themes = card(col, "Themes")
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var rowLayout: LinearLayout? = null
        Themes.all.forEachIndexed { i, t ->
            if (i % 2 == 0) {
                rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                grid.addView(rowLayout, LinearLayout.LayoutParams(-1, px(118)).apply { topMargin = px(8) })
            }
            val pv = ThemePreviewView(this, t, i)
            pv.setOnClickListener {
                prefs.followSystem = false
                prefs.themeIndex = i
                refreshPreviews()
            }
            previews.add(pv)
            rowLayout!!.addView(pv, LinearLayout.LayoutParams(0, -1, 1f).apply {
                marginEnd = if (i % 2 == 0) px(8) else 0
            })
        }
        themes.addView(grid)
        toggle(themes, "Follow phone light/dark mode", "Cute Pastel in light mode, Neon Teddy in dark mode",
            { prefs.followSystem }, { prefs.followSystem = it; refreshPreviews() })

        // ---- animations ----
        val anim = card(col, "Typing animations")
        toggle(anim, "Pop typing effects", "3D key bounce and floating letters", { prefs.effects }, { prefs.effects = it })
        toggle(anim, "Sparkles & hearts", "Particle effects on keys, Space, Enter, Delete and Emoji",
            { prefs.particles }, { prefs.particles = it })
        toggle(anim, "Extra effects", "Ripple rings, bear bounce, neon press outline and combo bursts every 10 fast keys",
            { prefs.extraEffects }, { prefs.extraEffects = it })

        // ---- keyboard ----
        val kb = card(col, "Keyboard")
        toggle(kb, "Number row", "Always show 1-0 above the letters", { prefs.numberRow }, { prefs.numberRow = it })
        toggle(kb, "Suggestions", "Word suggestions from a built-in list (on-device)", { prefs.suggestions }, { prefs.suggestions = it })
        toggle(kb, "Autocorrect", "Fix common typos when you press space", { prefs.autoCorrect }, { prefs.autoCorrect = it })
        seek(kb, "Keyboard height", 80, 130, { prefs.heightPercent }, { prefs.heightPercent = it }, { "$it%" })

        // ---- sound & vibration ----
        val sv = card(col, "Sound & vibration")
        sv.addView(label("Typing sound", 15f, white, false))
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val names = arrayOf("Off", "System click", "Teddy pop")
        for (i in names.indices) {
            rg.addView(RadioButton(this).apply {
                text = names[i]
                setTextColor(white)
                id = 100 + i
                isChecked = prefs.soundStyle == i
            })
        }
        rg.setOnCheckedChangeListener { _, id -> prefs.soundStyle = id - 100 }
        sv.addView(rg)
        seek(sv, "Sound volume", 0, 100, { prefs.soundVolume }, { prefs.soundVolume = it }, { "$it%" })
        toggle(sv, "Haptic feedback", "Short vibration on each key", { prefs.haptic }, { prefs.haptic = it })
        seek(sv, "Vibration strength", 5, 40, { prefs.hapticMs }, { prefs.hapticMs = it }, { "$it ms" })

        // ---- try it ----
        val tryIt = card(col, "Try it")
        tryIt.addView(EditText(this).apply {
            hint = "Type here to test MATHRA KEYBOARD..."
            setHintTextColor(0x99FFFFFF.toInt())
            setTextColor(white)
            minLines = 2
            gravity = Gravity.TOP
            background = GradientDrawable().apply {
                setColor(0x33000000)
                cornerRadius = px(14).toFloat()
                setStroke(px(1), 0x55FFC83D)
            }
            setPadding(px(12), px(10), px(12), px(10))
        })
        tryIt.addView(label("Tip: tap the bear to change theme, long-press it for settings. Hold Shift and swipe the " +
            "space bar to select text. Long-press the space bar or the globe key to switch keyboards.",
            12f, soft, false).apply { setPadding(0, px(8), 0, 0) })

        // ---- privacy ----
        val pr = card(col, "Privacy")
        pr.addView(label(
            "MATHRA KEYBOARD does not collect, store or send what you type, including passwords and private messages. " +
                "The app has no internet permission and no analytics. The only permission is Vibrate, used for key haptics " +
                "(you can turn it off). Suggestions use a built-in word list on your device and are switched off in " +
                "password, e-mail, URL and number fields.\n\n" +
                "Like every Android keyboard, MATHRA KEYBOARD can technically see keystrokes because that is how a keyboard works. " +
                "Android shows a warning when you enable any keyboard.",
            13f, soft, false))

        col.addView(label("MATHRA KEYBOARD v1.0.0  \u2022  Developed by MATHRA", 12f, soft, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, px(8), 0, 0)
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        status.text = when {
            current.startsWith(packageName) -> "\u2705 MATHRA Keyboard is enabled and selected"
            enabled.contains(packageName) -> "\u2714 Enabled. Now switch to it (step 2)"
            else -> "\u26A0 Not enabled yet. Start with step 1"
        }
        refreshPreviews()
    }

    private fun refreshPreviews() = previews.forEach { it.invalidate() }

    // ---------- small UI builders ----------
    private fun label(t: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun card(parent: LinearLayout, title: String): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0x22FFFFFF)
                cornerRadius = px(20).toFloat()
                setStroke(px(1), 0x33FFC83D)
            }
            setPadding(px(16), px(14), px(16), px(16))
        }
        c.addView(label(title, 17f, gold, true).apply { setPadding(0, 0, 0, px(6)) })
        parent.addView(c, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(14) })
        return c
    }

    private fun button(t: String, onClick: () -> Unit) = TextView(this).apply {
        text = t
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(0xFF1A1200.toInt())
        gravity = Gravity.CENTER
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFD86B.toInt(), 0xFFE8B22E.toInt())
        ).apply { cornerRadius = px(16).toFloat() }
        setPadding(px(12), px(12), px(12), px(12))
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(8) }
    }

    private fun toggle(parent: LinearLayout, title: String, desc: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, px(6), 0, px(6))
        }
        val tv = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tv.addView(label(title, 15f, white, false))
        tv.addView(label(desc, 12f, soft, false))
        row.addView(tv, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = get()
            setOnCheckedChangeListener { _, c -> set(c) }
        })
        parent.addView(row)
    }

    private fun seek(
        parent: LinearLayout, title: String, min: Int, max: Int,
        get: () -> Int, set: (Int) -> Unit, fmt: (Int) -> String
    ) {
        val tv = label("$title: ${fmt(get())}", 15f, white, false).apply { setPadding(0, px(8), 0, 0) }
        parent.addView(tv)
        parent.addView(SeekBar(this).apply {
            this.max = max - min
            progress = get() - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    set(p + min)
                    tv.text = "$title: ${fmt(p + min)}"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
    }

    // =====================================================================
    // Animated theme preview card
    // =====================================================================
    private inner class ThemePreviewView(ctx: Context, private val t: MTheme, private val index: Int) : View(ctx) {
        private val dp = resources.displayMetrics.density
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        private val rect = RectF()
        private var bg: Shader? = null
        private val counts = intArrayOf(7, 7, 5)

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            bg = LinearGradient(0f, 0f, 0f, h.toFloat(), t.bgTop, t.bgBottom, Shader.TileMode.CLAMP)
        }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val now = SystemClock.uptimeMillis()
            val corner = 14 * dp

            fill.shader = bg
            rect.set(0f, 0f, w, h)
            c.drawRoundRect(rect, corner, corner, fill)
            fill.shader = null

            // header: bear + theme name
            Art.drawBear(c, 20 * dp, 18 * dp, 9 * dp, true)
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = 11 * dp
            text.color = t.keyText
            text.textAlign = Paint.Align.LEFT
            c.drawText(t.name, 36 * dp, 22 * dp, text)
            text.textAlign = Paint.Align.CENTER

            // mini keys; one key at a time pops with a bounce and sends up a heart
            val pad = 8 * dp
            val gap = 3 * dp
            val keyW = (w - 2 * pad - 6 * gap) / 7f
            val top = 36 * dp
            val keyH = (h - top - pad - 2 * gap) / 3f
            val total = counts.sum()
            val slot = 420L
            val active = ((now / slot) % total).toInt()
            val u = (now % slot) / 1000f

            var n = 0
            for (r in 0..2) {
                val cnt = counts[r]
                val rowW = cnt * keyW + (cnt - 1) * gap
                var x = (w - rowW) / 2f
                val y = top + r * (keyH + gap)
                for (i in 0 until cnt) {
                    val isActive = n == active
                    val s = if (isActive) 1f + 0.28f * exp(-8f * u) * cos(20f * u) else 1f
                    val cx = x + keyW / 2f
                    val cy = y + keyH / 2f
                    c.save()
                    c.translate(cx, cy - (s - 1f) * keyH)
                    c.scale(s, s)
                    rect.set(-keyW / 2f, -keyH / 2f + 2 * dp, keyW / 2f, keyH / 2f)
                    fill.color = t.keySide
                    c.drawRoundRect(rect, 5 * dp, 5 * dp, fill)
                    rect.set(-keyW / 2f, -keyH / 2f, keyW / 2f, keyH / 2f - 2 * dp)
                    fill.color = if (isActive) t.spTop else t.keyTop
                    c.drawRoundRect(rect, 5 * dp, 5 * dp, fill)
                    c.restore()
                    if (isActive) {
                        val a = (255 * exp(-4f * u)).toInt()
                        text.typeface = Typeface.DEFAULT
                        text.textSize = 10 * dp
                        text.color = (t.sparkle[0] and 0x00FFFFFF) or (a shl 24)
                        c.drawText("\u2665", cx, cy - keyH * 0.6f - u * 60 * dp, text)
                    }
                    x += keyW + gap
                    n++
                }
            }

            // selection outline
            val selected = !prefs.followSystem && prefs.themeIndex == index
            if (selected) {
                stroke.strokeWidth = 3 * dp
                stroke.color = 0xFFFFC83D.toInt()
                rect.set(1.5f * dp, 1.5f * dp, w - 1.5f * dp, h - 1.5f * dp)
                c.drawRoundRect(rect, corner, corner, stroke)
            }

            postInvalidateDelayed(33)
        }
    }
}
