package com.shnwaz.lyrasave.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.shnwaz.lyrasave.MainActivity
import com.shnwaz.lyrasave.R
import com.shnwaz.lyrasave.databinding.FragmentSettingsBinding
import com.shnwaz.lyrasave.repository.StatusRepository
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private val repository by lazy { StatusRepository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDeveloperCard()
        setupSourceOptions()
        setupStorageAndCache()
    }

    private fun setupDeveloperCard() {
        // Load avatar with circle crop
        Glide.with(this)
            .load(R.drawable.avatar_shnwaz)
            .circleCrop()
            .into(b.ivDevAvatar)

        // Social Links
        b.btnDevGithub.setOnClickListener {
            openUrl("https://github.com/shnwazdeveloper")
        }

        b.btnDevTwitter.setOnClickListener {
            openUrl("https://x.com/shnwazdev")
        }

        b.btnDevInstagram.setOnClickListener {
            openUrl("https://instagram.com/shnwazxc")
        }

        b.btnDevWebsite.setOnClickListener {
            openUrl("https://shnwaz.dev")
        }
    }

    private fun setupSourceOptions() {
        updateSourceUi(repository.getSourceType())

        b.optionWhatsApp.setOnClickListener {
            repository.setSourceType(StatusRepository.SOURCE_WHATSAPP)
            updateSourceUi(StatusRepository.SOURCE_WHATSAPP)
            (activity as? MainActivity)?.showAccessInstructionDialog(isWaBusiness = false)
        }

        b.optionWhatsAppBusiness.setOnClickListener {
            repository.setSourceType(StatusRepository.SOURCE_WHATSAPP_BUSINESS)
            updateSourceUi(StatusRepository.SOURCE_WHATSAPP_BUSINESS)
            (activity as? MainActivity)?.showAccessInstructionDialog(isWaBusiness = true)
        }

        b.optionAutoDetect.setOnClickListener {
            (activity as? MainActivity)?.performAutoDetect()
        }
    }

    private fun updateSourceUi(selectedType: Int) {
        if (selectedType == StatusRepository.SOURCE_WHATSAPP_BUSINESS) {
            b.ivCheckWa.visibility = View.GONE
            b.ivCheckWab.visibility = View.VISIBLE
        } else {
            b.ivCheckWa.visibility = View.VISIBLE
            b.ivCheckWab.visibility = View.GONE
        }
    }

    private fun setupStorageAndCache() {
        b.optionClearCache.setOnClickListener {
            val ok = repository.clearAppCache()
            val msg = if (ok) getString(R.string.settings_cache_cleared) else "Cache is already empty"
            Snackbar.make(b.root, msg, Snackbar.LENGTH_SHORT).show()
        }

        b.optionCheckUpdate.setOnClickListener {
            b.tvUpdateStatus.text = "Checking for updates..."
            viewLifecycleOwner.lifecycleScope.launch {
                com.shnwaz.lyrasave.utils.UpdateManager.checkUpdate(requireContext(), isManual = true, anchorView = b.root)
                b.tvUpdateStatus.text = "Check GitHub releases for latest version"
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(b.root, "Could not open link: $url", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
