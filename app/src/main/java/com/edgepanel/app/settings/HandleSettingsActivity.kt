package com.edgepanel.app.settings

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edgepanel.app.databinding.ActivityHandleSettingsBinding
import com.edgepanel.app.model.HandleConfig
import com.edgepanel.app.service.EdgePanelService
import com.edgepanel.app.util.PrefsManager

class HandleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHandleSettingsBinding
    private lateinit var prefs: PrefsManager
    private var config: HandleConfig = HandleConfig()

    private val COLORS = listOf(
        0xFF6200EE.toInt() to "Purple",
        0xFF3700B3.toInt() to "Deep Purple",
        0xFF03DAC5.toInt() to "Teal",
        0xFF018786.toInt() to "Dark Teal",
        0xFFE91E63.toInt() to "Pink",
        0xFFF44336.toInt() to "Red",
        0xFFFF9800.toInt() to "Orange",
        0xFFFFEB3B.toInt() to "Yellow",
        0xFF4CAF50.toInt() to "Green",
        0xFF2196F3.toInt() to "Blue",
        0xFF000000.toInt() to "Black",
        0xFFFFFFFF.toInt() to "White"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHandleSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs  = PrefsManager(this)
        config = prefs.getHandleConfig()

        loadSettings()
        wireListeners()
        updatePreview()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadSettings() {
        binding.togglePosition.isChecked = config.isLeft
        binding.tvPositionLabel.text     = if (config.isLeft) "Left side" else "Right side"
        binding.seekTransparency.progress = (config.transparency * 100).toInt()
        binding.seekHeight.progress       = ((config.height - 80) / 4).coerceIn(0, 100)
        binding.seekWidth.progress        = ((config.width  - 20) / 2).coerceIn(0, 100)
        binding.tvTransparencyVal.text    = "${(config.transparency * 100).toInt()}%"
        buildColorPicker()
    }

    private fun wireListeners() {
        binding.togglePosition.setOnCheckedChangeListener { _, checked ->
            config = config.copy(isLeft = checked)
            binding.tvPositionLabel.text = if (checked) "Left side" else "Right side"
            updatePreview()
        }

        binding.seekTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, byUser: Boolean) {
                config = config.copy(transparency = p / 100f)
                binding.tvTransparencyVal.text = "$p%"
                updatePreview()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar)  {}
        })

        binding.seekHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, byUser: Boolean) {
                config = config.copy(height = 80 + p * 4)
                updatePreview()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar)  {}
        })

        binding.seekWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, byUser: Boolean) {
                config = config.copy(width = 20 + p * 2)
                updatePreview()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar)  {}
        })

        binding.btnSave.setOnClickListener { save() }
    }

    private fun buildColorPicker() {
        binding.colorGrid.removeAllViews()
        COLORS.forEach { (color, name) ->
            val cell = FrameLayout(this).apply {
                val dp36 = (36 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(dp36, dp36).also {
                    it.marginEnd = 16; it.bottomMargin = 16
                }
                val swatch = TextView(this@HandleSettingsActivity).apply {
                    setBackgroundColor(color)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    if (color == 0xFFFFFFFF.toInt())
                        setBackgroundResource(android.R.drawable.btn_default)
                }
                addView(swatch)
                if (config.color == color) {
                    // Highlight ring
                    val ring = TextView(this@HandleSettingsActivity).apply {
                        setBackgroundResource(android.R.drawable.btn_default)
                        background?.alpha = 0
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(0x44FFFFFF.toInt())
                    }
                    addView(ring)
                }
                setOnClickListener {
                    config = config.copy(color = color)
                    buildColorPicker(); updatePreview()
                }
                tooltipText = name
            }
            binding.colorGrid.addView(cell)
        }
    }

    private fun updatePreview() {
        val pv = binding.handlePreview
        val bm = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bm)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = config.color
            alpha      = (config.transparency * 255f).toInt()
        }
        val r = config.cornerRadius
        val path = Path()
        val rect = RectF(0f, 0f, config.width.toFloat(), config.height.toFloat())
        val radii = if (config.isLeft)
            floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
        else
            floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
        path.addRoundRect(rect, radii, Path.Direction.CW)
        cv.drawPath(path, paint)
        pv.setImageBitmap(bm)
    }

    private fun save() {
        prefs.saveHandleConfig(config)
        if (EdgePanelService.isRunning) {
            startService(Intent(this, EdgePanelService::class.java).apply {
                action = EdgePanelService.ACTION_STOP
            })
            android.os.Handler(mainLooper).postDelayed({
                startForegroundService(Intent(this, EdgePanelService::class.java))
            }, 500)
        }
        Toast.makeText(this, "Handle settings saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
