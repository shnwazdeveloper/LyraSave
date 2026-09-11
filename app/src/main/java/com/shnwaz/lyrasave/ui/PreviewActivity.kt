package com.shnwaz.lyrasave.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.shnwaz.lyrasave.R
import com.shnwaz.lyrasave.databinding.ActivityPreviewBinding
import com.shnwaz.lyrasave.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.shnwaz.lyrasave.utils.MediaUtils

class PreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI      = "extra_uri"
        const val EXTRA_IS_VIDEO = "extra_is_video"
        const val EXTRA_NAME     = "extra_name"
        const val EXTRA_IS_SAVED = "extra_is_saved"
    }

    private lateinit var binding: ActivityPreviewBinding
    private var player: ExoPlayer? = null
    private val repository by lazy { StatusRepository(this) }

    private lateinit var currentUri: Uri
    private lateinit var fileName: String
    private var isVideo = false
    private var isSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Status Bar insets for topBar
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingStart, statusBars.top + 8, v.paddingEnd, v.paddingBottom)
            insets
        }

        // Handle Navigation Bar insets for bottomActionBar
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionBar) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingStart, v.paddingTop, v.paddingEnd, navBars.bottom + 16)
            insets
        }

        val uriString = intent.getStringExtra(EXTRA_URI) ?: run { finish(); return }
        currentUri = Uri.parse(uriString)
        isVideo    = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
        fileName   = intent.getStringExtra(EXTRA_NAME) ?: "status_file"
        isSaved    = intent.getBooleanExtra(EXTRA_IS_SAVED, false)

        if (!isSaved) {
            isSaved = repository.isStatusAlreadySaved(fileName)
        }

        binding.tvFileName.text = fileName
        updateSaveButton()

        if (isVideo) showVideo(currentUri) else showImage(currentUri)

        binding.btnBack.setOnClickListener  { onBackPressedDispatcher.onBackPressed() }
        binding.btnSave.setOnClickListener  { saveFile() }
        binding.btnShare.setOnClickListener { shareFile() }
    }

    private fun showImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.ivPreview.visibility   = View.VISIBLE
        binding.playerView.visibility  = View.GONE

        Glide.with(this)
            .load(uri)
            .error(R.drawable.ic_folder_off)
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.progressBar.visibility = View.GONE
                    return false
                }
            })
            .into(binding.ivPreview)
    }

    private fun showVideo(uri: Uri) {
        binding.progressBar.visibility = View.GONE
        binding.ivPreview.visibility   = View.GONE
        binding.playerView.visibility  = View.VISIBLE

        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    private fun updateSaveButton() {
        if (isSaved) {
            binding.btnSave.text = getString(R.string.saved)
            binding.btnSave.setIconResource(R.drawable.ic_check_circle)
            binding.btnSave.isEnabled = false
            binding.btnSave.alpha = 0.6f
        } else {
            binding.btnSave.text = getString(R.string.save)
            binding.btnSave.setIconResource(R.drawable.ic_download)
            binding.btnSave.isEnabled = true
            binding.btnSave.alpha = 1.0f
        }
    }

    private fun saveFile() {
        lifecycleScope.launch {
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Saving..."
            val ok = withContext(Dispatchers.IO) {
                repository.saveStatus(currentUri, fileName, isVideo)
            }
            if (ok) {
                isSaved = true
                updateSaveButton()
                val targetDir = if (isVideo) "Movies/LyraSave" else "Pictures/LyraSave"
                Snackbar.make(
                    binding.root,
                    "Saved to $targetDir",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = getString(R.string.save)
                Snackbar.make(
                    binding.root,
                    getString(R.string.save_failed),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun shareFile() {
        lifecycleScope.launch {
            binding.btnShare.isEnabled = false
            val shareUri = withContext(Dispatchers.IO) {
                MediaUtils.createShareUri(this@PreviewActivity, currentUri, fileName)
            }
            binding.btnShare.isEnabled = true
            if (shareUri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (isVideo) "video/*" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
            } else {
                Snackbar.make(binding.root, getString(R.string.share_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}