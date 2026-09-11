package com.shnwaz.lyrasave.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.shnwaz.lyrasave.MainActivity
import com.shnwaz.lyrasave.R
import com.shnwaz.lyrasave.adapter.StatusAdapter
import com.shnwaz.lyrasave.databinding.FragmentStatusListBinding
import com.shnwaz.lyrasave.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SavedFragment : Fragment() {

    private var _b: FragmentStatusListBinding? = null
    private val b get() = _b!!

    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }
    private lateinit var adapter: StatusAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentStatusListBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StatusAdapter(
            onItemClick   = ::openPreview,
            onSaveClick   = { /* already saved; no-op */ },
            onShareClick  = ::shareItem
        )

        b.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter  = this@SavedFragment.adapter
            setHasFixedSize(true)
            addItemDecoration(GridSpacingItemDecoration(spanCount = 2, spacingDp = 8))
        }

        b.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadSavedStatuses()
        }
        b.swipeRefreshLayout.setColorSchemeResources(R.color.md_theme_primary)

        viewModel.savedStatuses.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            b.swipeRefreshLayout.isRefreshing = false
            toggleEmptyState(items.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading && adapter.currentList.isEmpty()) {
                com.shnwaz.lyrasave.utils.SkeletonHelper.start(b.layoutSkeleton.root)
                b.layoutEmpty.visibility = View.GONE
            } else {
                com.shnwaz.lyrasave.utils.SkeletonHelper.stop(b.layoutSkeleton.root)
                toggleEmptyState(adapter.currentList.isEmpty())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSavedStatuses()
    }

    private fun toggleEmptyState(empty: Boolean) {
        b.layoutEmpty.visibility  = if (empty) View.VISIBLE else View.GONE
        b.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        b.tvEmptyTitle.text    = getString(R.string.no_saved_yet)
        b.tvEmptySubtitle.text = getString(R.string.no_saved_subtitle)
    }

    private fun openPreview(item: StatusItem) {
        startActivity(
            Intent(requireContext(), PreviewActivity::class.java).apply {
                putExtra(PreviewActivity.EXTRA_URI,      item.uri.toString())
                putExtra(PreviewActivity.EXTRA_IS_VIDEO, item.isVideo)
                putExtra(PreviewActivity.EXTRA_NAME,     item.name)
                putExtra(PreviewActivity.EXTRA_IS_SAVED, true)
            }
        )
    }

    private fun shareItem(item: StatusItem) {
        lifecycleScope.launch {
            val shareUri = withContext(Dispatchers.IO) {
                com.shnwaz.lyrasave.utils.MediaUtils.createShareUri(requireContext(), item.uri, item.name)
            }
            if (shareUri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (item.isVideo) "video/*" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
            } else {
                Snackbar.make(b.root, getString(R.string.share_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}