package io.legado.app.ui.book.read.immersive

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.help.book.BookHelp
import io.legado.app.ui.ai.AIDialog
import io.legado.app.ui.ai.DeepSeekClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
class ImmersiveReadActivity : AppCompatActivity() {
    private lateinit var book: Book
    private var fontSize = 18f
    private var lineSpacing = 8f
    private var isNightMode = false

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
        setContentView(R.layout.activity_immersive_read)
        book = intent.getSerializableExtra(EXTRA_BOOK) as Book
        setupToolbar()
        setupFontControls()
        setupBottomButtons()
        loadChapters()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = book.name
        toolbar.setOnClickListener { toggleControls() }
    }

    private fun setupFontControls() {
        val fontSizeSeekbar = findViewById<SeekBar>(R.id.font_size_seekbar)
        val lineSpacingSeekbar = findViewById<SeekBar>(R.id.line_spacing_seekbar)

        fontSizeSeekbar.progress = (fontSize - 12f).toInt()
        fontSizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, f: Boolean) {
                fontSize = 12f + v
                updateTextAppearance()
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })

        lineSpacingSeekbar.progress = (lineSpacing - 2f).toInt()
        lineSpacingSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, f: Boolean) {
                lineSpacing = 2f + v
                updateTextAppearance()
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })

        findViewById<TextView>(R.id.btn_font_decrease).setOnClickListener {
            if (fontSize > 12f) { fontSize -= 1f; fontSizeSeekbar.progress = (fontSize - 12f).toInt(); updateTextAppearance() }
        }
        findViewById<TextView>(R.id.btn_font_increase).setOnClickListener {
            if (fontSize < 42f) { fontSize += 1f; fontSizeSeekbar.progress = (fontSize - 12f).toInt(); updateTextAppearance() }
        }
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btn_ai).setOnClickListener { showAIDialog() }
        findViewById<TextView>(R.id.btn_night_mode).setOnClickListener { toggleNightMode() }
        findViewById<TextView>(R.id.btn_settings).setOnClickListener { showSettings() }
    }

    private fun loadChapters() {
        lifecycleScope.launch {
            val chapters = withContext(Dispatchers.IO) {
                val bookChapters = appDb.bookChapterDao.getChapterList(book.bookUrl)
                bookChapters.mapNotNull { bc ->
                    val content = BookHelp.getContent(book, bc)
                    if (content != null) ImmersiveChapter(bc.title, content)
                    else ImmersiveChapter(bc.title, "(loading...)")
                }
            }
            if (chapters.isNotEmpty()) {
                val viewPager = findViewById<ViewPager2>(R.id.view_pager)
                viewPager.adapter = ImmersiveReadAdapter(this@ImmersiveReadActivity, chapters, fontSize, lineSpacing, isNightMode)
                viewPager.offscreenPageLimit = 1
                viewPager.currentItem = book.durChapterIndex.coerceAtMost(chapters.size - 1)
            }
        }
    }

    private fun updateTextAppearance() {
        val vp = findViewById<ViewPager2>(R.id.view_pager)
        (vp.adapter as? ImmersiveReadAdapter)?.let { a ->
            a.updateFontSize(fontSize)
            a.updateLineSpacing(lineSpacing)
        }
    }

    private fun toggleControls() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val bottom = findViewById<android.view.View>(R.id.bottom_panel)
        val showing = toolbar.visibility == android.view.View.VISIBLE
        toolbar.visibility = if (showing) android.view.View.GONE else android.view.View.VISIBLE
        bottom.visibility = if (showing) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode
        findViewById<android.widget.LinearLayout>(R.id.root_layout).setBackgroundColor(
            resources.getColor(if (isNightMode) R.color.read_bg_night else R.color.read_bg, theme)
        )
        findViewById<TextView>(R.id.btn_night_mode).text = if (isNightMode) "\u65e5\u95f4" else "\u591c\u95f4"
        val vp = findViewById<ViewPager2>(R.id.view_pager)
        (vp.adapter as? ImmersiveReadAdapter)?.updateNightMode(isNightMode)
    }

    private fun showAIDialog() {
        val vp = findViewById<ViewPager2>(R.id.view_pager)
        val text = (vp.adapter as? ImmersiveReadAdapter)?.getPageText(vp.currentItem) ?: ""
        AIDialog.showDialog(supportFragmentManager, text)
    }

    private fun showSettings() {
        val apiKey = getSharedPreferences("io.legado.app_preferences", MODE_PRIVATE)
            .getString(PreferKey.deepseekApiKey, "") ?: ""
        val input = android.widget.EditText(this).apply {
            setText(apiKey)
            setHint(R.string.deepseek_api_key_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_feature)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val key = input.text.toString().trim()
                getSharedPreferences("io.legado.app_preferences", MODE_PRIVATE)
                    .edit().putString(PreferKey.deepseekApiKey, key).apply()
                if (key.isNotBlank()) DeepSeekClient.updateConfig(DeepSeekClient.getCurrentConfig().copy(apiKey = key))
                android.widget.Toast.makeText(this, "API Key saved", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
