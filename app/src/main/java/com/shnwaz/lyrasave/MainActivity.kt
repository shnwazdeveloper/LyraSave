package com.shnwaz.lyrasave

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shnwaz.lyrasave.databinding.ActivityMainBinding
import com.shnwaz.lyrasave.repository.StatusRepository
import com.shnwaz.lyrasave.ui.SavedFragment
import com.shnwaz.lyrasave.ui.SettingsFragment
import com.shnwaz.lyrasave.ui.StatusListFragment
import com.shnwaz.lyrasave.viewmodel.StatusViewModel
import com.shnwaz.lyrasave.viewmodel.StatusViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val repository by lazy { StatusRepository(this) }

    val viewModel: StatusViewModel by viewModels {
        StatusViewModelFactory(repository)
    }

    // SAF folder picker launcher
    private val openDocumentTree = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            repository.persistUri(uri)
            repository.setOnboardingCompleted(true)
            viewModel.loadStatuses(uri) { count ->
                showMainContent()
                val msg = if (count > 0) "$count statuses found!" else "Folder selected! Pull down to refresh anytime."
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(binding.root, getString(R.string.permission_denied), Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBottomNavigation()
        setupPermissionButtons()
        observePermissionRevoked()
        checkAndInitAccess()
        checkAppUpdate()
    }

    private fun checkAppUpdate() {
        lifecycleScope.launch {
            com.shnwaz.lyrasave.utils.UpdateManager.checkUpdate(this@MainActivity, isManual = false)
        }
    }

    override fun onResume() {
        super.onResume()
        // If All Files Access is granted, auto-detect immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            repository.setOnboardingCompleted(true)
            viewModel.loadStatuses(null) { count ->
                if (count > 0) {
                    showMainContent()
                }
            }
        }
    }

    fun checkAndInitAccess() {
        if (repository.isOnboardingCompleted()) {
            showMainContent()
            val savedUri = repository.getPersistedUri()
            viewModel.loadStatuses(savedUri)
            return
        }

        // Attempt quick auto-detection from device storage first
        viewModel.loadStatuses(null) { count ->
            if (count > 0) {
                repository.setOnboardingCompleted(true)
                showMainContent()
            } else {
                val savedUri = repository.getPersistedUri()
                if (savedUri != null && isUriPermissionValid(savedUri)) {
                    repository.setOnboardingCompleted(true)
                    viewModel.loadStatuses(savedUri)
                    showMainContent()
                } else {
                    showPermissionScreen()
                }
            }
        }
    }

    fun performAutoDetect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Allow All Files Access")
                .setMessage("To auto-detect and display WhatsApp statuses automatically without manual folder selection, please enable 'Allow access to manage all files' for Lyra Save.")
                .setIcon(R.drawable.ic_app_logo)
                .setPositiveButton("Open Settings") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        binding.btnAutoDetect.isEnabled = false
        Snackbar.make(binding.root, getString(R.string.auto_detecting), Snackbar.LENGTH_SHORT).show()
        viewModel.loadStatuses(null) { count ->
            binding.btnAutoDetect.isEnabled = true
            repository.setOnboardingCompleted(true)
            showMainContent()
            if (count > 0) {
                Snackbar.make(binding.root, "$count statuses found & loaded!", Snackbar.LENGTH_SHORT).show()
            } else {
                val savedUri = repository.getPersistedUri()
                if (savedUri != null && isUriPermissionValid(savedUri)) {
                    viewModel.loadStatuses(savedUri)
                } else {
                    Snackbar.make(binding.root, "No statuses found. Open WhatsApp and view some statuses first!", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun isUriPermissionValid(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_images   -> { showFragment(FragmentType.IMAGES); true }
                R.id.nav_videos   -> { showFragment(FragmentType.VIDEOS); true }
                R.id.nav_saved    -> { showFragment(FragmentType.SAVED);  true }
                R.id.nav_settings -> { showFragment(FragmentType.SETTINGS); true }
                else -> false
            }
        }
    }

    private fun setupPermissionButtons() {
        binding.btnAutoDetect.setOnClickListener {
            performAutoDetect()
        }
        binding.btnGrantAccessWa.setOnClickListener {
            showAccessInstructionDialog(isWaBusiness = false)
        }
        binding.btnGrantAccessWaBusiness.setOnClickListener {
            showAccessInstructionDialog(isWaBusiness = true)
        }
        binding.btnRefreshPermission.setOnClickListener {
            repository.setOnboardingCompleted(true)
            showMainContent()
            viewModel.loadStatuses(repository.getPersistedUri())
            Snackbar.make(binding.root, "Access verified. Loading statuses...", Snackbar.LENGTH_SHORT).show()
        }
    }

    fun showAccessInstructionDialog(isWaBusiness: Boolean) {
        val appName = if (isWaBusiness) "WhatsApp Business" else "WhatsApp"
        val packageName = if (isWaBusiness) "com.whatsapp.w4b" else "com.whatsapp"
        val folderName = if (isWaBusiness) "WhatsApp Business" else "WhatsApp"

        val message = "In the next screen, choose either:\n\n" +
                "1. The '.Statuses' folder (Tap 3-dots at top right and choose 'Show hidden files')\n\n" +
                "2. OR simply select the '$folderName' or 'Media' folder\n\n" +
                "Lyra Save will automatically detect and load all statuses inside it!"

        MaterialAlertDialogBuilder(this)
            .setTitle("Grant $appName Access")
            .setMessage(message)
            .setIcon(R.drawable.ic_app_logo)
            .setPositiveButton("Open Folder Picker") { _, _ ->
                launchFolderPicker(isWaBusiness)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchFolderPicker(isWaBusiness: Boolean) {
        val packageName = if (isWaBusiness) "com.whatsapp.w4b" else "com.whatsapp"
        val folderName = if (isWaBusiness) "WhatsApp Business" else "WhatsApp"

        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "Android/media/$packageName/$folderName/Media"
        } else {
            "$folderName/Media"
        }

        val initialUri = try {
            val encoded = path.replace("/", "%2F")
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$encoded")
        } catch (e: Exception) {
            null
        }

        openDocumentTree.launch(initialUri)
    }

    private fun observePermissionRevoked() {
        viewModel.permissionRevoked.observe(this) { revoked ->
            if (revoked == true) {
                repository.clearPersistedUri()
                showPermissionScreen()
                viewModel.resetPermissionRevoked()
                Snackbar.make(binding.root, getString(R.string.permission_revoked), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun showMainContent() {
        binding.layoutPermission.visibility = View.GONE
        binding.layoutMain.visibility = View.VISIBLE
        if (supportFragmentManager.findFragmentByTag(FragmentType.IMAGES.tag) == null &&
            supportFragmentManager.findFragmentByTag(FragmentType.VIDEOS.tag) == null &&
            supportFragmentManager.findFragmentByTag(FragmentType.SAVED.tag) == null &&
            supportFragmentManager.findFragmentByTag(FragmentType.SETTINGS.tag) == null) {
            showFragment(FragmentType.IMAGES)
            binding.bottomNavigation.selectedItemId = R.id.nav_images
        }
    }

    private fun showPermissionScreen() {
        if (repository.isOnboardingCompleted()) {
            showMainContent()
            return
        }
        binding.layoutMain.visibility = View.GONE
        binding.layoutPermission.visibility = View.VISIBLE
    }

    private fun showFragment(type: FragmentType) {
        val tag = type.tag
        val tx = supportFragmentManager.beginTransaction()
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

        // Hide all current fragments
        supportFragmentManager.fragments.forEach { tx.hide(it) }

        val existing = supportFragmentManager.findFragmentByTag(tag)
        if (existing != null) {
            tx.show(existing)
        } else {
            val fragment = when (type) {
                FragmentType.IMAGES   -> StatusListFragment.newImages()
                FragmentType.VIDEOS   -> StatusListFragment.newVideos()
                FragmentType.SAVED    -> SavedFragment()
                FragmentType.SETTINGS -> SettingsFragment()
            }
            tx.add(R.id.fragmentContainer, fragment, tag)
        }
        tx.commit()
    }

    enum class FragmentType(val tag: String) {
        IMAGES("images"),
        VIDEOS("videos"),
        SAVED("saved"),
        SETTINGS("settings")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                val uri = repository.getPersistedUri()
                viewModel.loadStatuses(uri)
                Snackbar.make(binding.root, getString(R.string.refreshing), Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_change_source -> {
                showAccessInstructionDialog(isWaBusiness = false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
