package com.edgepanel.app

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.edgepanel.app.databinding.ActivityMainBinding
import com.edgepanel.app.service.EdgePanelService
import com.edgepanel.app.settings.HandleSettingsActivity
import com.edgepanel.app.util.PermissionHelper
import com.edgepanel.app.util.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        prefs = PrefsManager(this)
        wire()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        syncServiceSwitch()
    }

    private fun wire() {
        // Service toggle
        binding.switchService.setOnCheckedChangeListener { _, on ->
            if (on) {
                if (!PermissionHelper.hasOverlayPermission(this)) {
                    binding.switchService.isChecked = false
                    Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission(); return@setOnCheckedChangeListener
                }
                prefs.setServiceEnabled(true)
                startForegroundService(Intent(this, EdgePanelService::class.java))
            } else {
                prefs.setServiceEnabled(false)
                startService(Intent(this, EdgePanelService::class.java).apply { action = EdgePanelService.ACTION_STOP })
            }
        }

        // Permission cards
        binding.cardOverlay.setOnClickListener   { requestOverlayPermission() }
        binding.cardFreeform.setOnClickListener  {
            Toast.makeText(this, "Run the ADB command in a terminal connected to your device", Toast.LENGTH_LONG).show()
        }
        binding.btnCopyAdb.setOnClickListener    { copyAdb() }

        // Nav cards
        binding.cardHandleSettings.setOnClickListener {
            startActivity(Intent(this, HandleSettingsActivity::class.java))
        }
        binding.cardPanelVisibility.setOnClickListener {
            showPanelVisibilityDialog()
        }
        binding.btnAbout.setOnClickListener {
            Toast.makeText(this, "Edge Panel v1.0 — Long-press app icons in the panel to pin them", Toast.LENGTH_LONG).show()
        }
    }

    private fun syncServiceSwitch() {
        binding.switchService.setOnCheckedChangeListener(null)
        binding.switchService.isChecked = EdgePanelService.isRunning
        binding.switchService.setOnCheckedChangeListener { _, on ->
            if (on) {
                if (!PermissionHelper.hasOverlayPermission(this)) {
                    binding.switchService.isChecked = false
                    requestOverlayPermission(); return@setOnCheckedChangeListener
                }
                prefs.setServiceEnabled(true)
                startForegroundService(Intent(this, EdgePanelService::class.java))
            } else {
                prefs.setServiceEnabled(false)
                startService(Intent(this, EdgePanelService::class.java).apply { action = EdgePanelService.ACTION_STOP })
            }
        }
    }

    private fun refreshStatus() {
        val hasOverlay  = PermissionHelper.hasOverlayPermission(this)
        val hasFreeform = PermissionHelper.hasFreeformPermission(this)

        fun color(ok: Boolean) = if (ok) getColor(R.color.perm_ok) else getColor(R.color.perm_missing)

        binding.tvOverlayStatus.text      = if (hasOverlay)  "✓ Granted"           else "✗ Tap to grant"
        binding.tvOverlayStatus.setTextColor(color(hasOverlay))
        binding.tvFreeformStatus.text     = if (hasFreeform) "✓ Granted"           else "✗ ADB required"
        binding.tvFreeformStatus.setTextColor(color(hasFreeform))
        binding.tvFgServiceStatus.text    = "✓ Registered"
        binding.tvFgServiceStatus.setTextColor(color(true))
        binding.tvBootStatus.text         = "✓ Auto-start registered"
        binding.tvBootStatus.setTextColor(color(true))

        binding.tvAdbCommand.text = PermissionHelper.adbGrantCommand(packageName)

        // Auto-start if prefs say so and permissions are ready
        if (hasOverlay && prefs.isServiceEnabled() && !EdgePanelService.isRunning) {
            startForegroundService(Intent(this, EdgePanelService::class.java))
        }
    }

    private fun requestOverlayPermission() =
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

    private fun copyAdb() {
        val cmd = PermissionHelper.adbGrantCommand(packageName)
        val cb  = getSystemService(ClipboardManager::class.java)
        cb.setPrimaryClip(ClipData.newPlainText("ADB command", cmd))
        Toast.makeText(this, "ADB command copied!", Toast.LENGTH_SHORT).show()
    }

    private fun showPanelVisibilityDialog() {
        val all     = arrayOf("Apps", "Tasks", "People", "Weather")
        val visible = prefs.getVisiblePanels()
        val checked = all.map { it in visible }.toBooleanArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Visible Panels")
            .setMultiChoiceItems(all, checked) { _, i, v -> checked[i] = v }
            .setPositiveButton("Save") { _, _ ->
                val selected = all.filterIndexed { i, _ -> checked[i] }.toSet()
                if (selected.isEmpty()) {
                    Toast.makeText(this, "Select at least one panel", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.setVisiblePanels(selected)
                    Toast.makeText(this, "Restart the service to apply", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
