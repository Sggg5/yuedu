package io.legado.app.ui.book.read.immersive

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.ui.ai.DeepSeekClient
import io.legado.app.data.entities.Book
import io.legado.app.ui.ai.AIDialog
import io.legado.app.databinding.ActivityImmersiveReadBinding
import io.legado.app.ui.book.read.ReadBookViewModel
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ClickableViewAccessibility")
class ImmersiveReadActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityImmersiveReadBinding::inflate, setContentView = true)
    private val viewModel by viewModels<ReadBookViewModel>()
    private lateinit var book: Book
    private var fontSize = 18f
    private var lineSpacing = 8f
    private var isNightMode = false
    private var isControlsVisible = true

    companion object {
        const val EXTRA_BOOK = "book"
        fun start(context: Context, book: Book) {
            context.startActivity(Intent(context, ImmersiveReadActivity::class.java).apply {
                putExtra(EXTRA_BOOK, book)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        book = intent.getParcelableExtra<Book>(EXTRA_BOOK)!!
        initView()
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = book.name

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {}
        })

        binding.rootLayout.setOnClickListener { toggleControls() }

        binding.fontSizeSeekbar.progress = (fontSize - 12f).toInt()
        binding.fontSizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, value: Int, fromUser: Boolean) {
                fontSize = 12f + value; updatePageTextSize()
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
        binding.btnFontDecrease.setOnClickListener {
            if (fontSize > 12f) { fontSize -= 1f; binding.fontSizeSeekbar.progress = (fontSize - 12f).toInt(); updatePageTextSize() }
        }
        binding.btnFontIncrease.setOnClickListener {
            if (fontSize < 42f) { fontSize += 1f; binding.fontSizeSeekbar.progress = (fontSize - 12f).toInt(); updatePageTextSize() }
        }

        binding.lineSpacingSeekbar.progress = (lineSpacing - 2f).toInt()
        binding.lineSpacingSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, value: Int, fromUser: Boolean) {
                lineSpacing = 2f + value; updatePageLineSpacing()
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        binding.btnAi.setOnClickListener { showAIDialog() }
        binding.btnNightMode.setOnClickListener { toggleNightMode() }
        binding.btnSettings.setOnClickListener { showSettings() }

        loadContent()
    }

    private fun loadContent() {
        lifecycleScope.launch {
            val chapters = withContext(Dispatchers.IO) {
                val db = io.legado.app.data.appDb
                val bookChapters = db.bookChapterDao.getChapterList(book.bookUrl)
                bookChapters.mapNotNull { bc ->
                    val content = io.legado.app.help.book.BookHelp.getContent(book, bc)
                    ImmersiveChapter(
                        bc.title,
                        content ?: "(content loading, please cache chapter first)"
                    )
                }
            }
            if (chapters.isNotEmpty()) {
                setupViewPager(chapters)
            }
        }
    }

    private fun setupViewPager(chapters: List<ImmersiveChapter>) {
        val adapter = ImmersiveReadAdapter(this, chapters, fontSize, lineSpacing, isNightMode)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.currentItem = book.durChapterIndex.coerceAtMost(chapters.size - 1)
    }

    private fun toggleControls() {
        isControlsVisible = !isControlsVisible
        val alphaTarget = if (isControlsVisible) 1f else 0f
        ValueAnimator.ofFloat(1f - alphaTarget, alphaTarget).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val alpha = anim.animatedValue as Float
                binding.toolbar.alpha = alpha
                binding.toolbar.isVisible = isControlsVisible
                binding.bottomPanel.alpha = alpha
                binding.bottomPanel.isVisible = isControlsVisible
            }
            start()
        }
    }

    private fun updatePageTextSize() {
        (binding.viewPager.adapter as? ImmersiveReadAdapter)?.updateFontSize(fontSize)
    }

    private fun updatePageLineSpacing() {
        (binding.viewPager.adapter as? ImmersiveReadAdapter)?.updateLineSpacing(lineSpacing)
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode
        binding.rootLayout.setBackgroundColor(
            resources.getColor(
                if (isNightMode) R.color.read_bg_night else R.color.read_bg,
                theme
            )
        )
        binding.btnNightMode.text = if (isNightMode) "Day" else "Night"
        (binding.viewPager.adapter as? ImmersiveReadAdapter)?.updateNightMode(isNightMode)
    }

    private fun showAIDialog() {
        val text = (binding.viewPager.adapter as? ImmersiveReadAdapter)
            ?.getPageText(binding.viewPager.currentItem) ?: ""
        AIDialog.showDialog(supportFragmentManager, text)
    }

    private fun showSettings() {
        val apiKey = getSharedPreferences("io.legado.app_preferences", MODE_PRIVATE)
            .getString(PreferKey.deepseekApiKey, "") ?: ""
        val input = android.widget.EditText(this).apply {
            setText(apiKey)
            setHint(R.string.deepseek_api_key_hint)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.ai_feature)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val key = input.text.toString().trim()
                getSharedPreferences("io.legado.app_preferences", MODE_PRIVATE)
                    .edit()
                    .putString(PreferKey.deepseekApiKey, key)
                    .apply()
                if (key.isNotBlank()) {
                    DeepSeekClient.updateConfig(DeepSeekClient.getCurrentConfig().copy(apiKey = key))
                }
                Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        toggleControls()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}

