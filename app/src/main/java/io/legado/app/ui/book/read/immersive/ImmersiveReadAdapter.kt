package io.legado.app.ui.book.read.immersive

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ImmersiveReadAdapter(
    activity: FragmentActivity,
    private var chapters: List<ImmersiveChapter>,
    private var fontSize: Float,
    private var lineSpacing: Float,
    private var isNightMode: Boolean
) : FragmentStateAdapter(activity) {

    private val pageTextCache = mutableMapOf<Int, String>()

    fun updateFontSize(size: Float) {
        fontSize = size
        notifyDataSetChanged()
    }

    fun updateLineSpacing(spacing: Float) {
        lineSpacing = spacing
        notifyDataSetChanged()
    }

    fun updateNightMode(night: Boolean) {
        isNightMode = night
        notifyDataSetChanged()
    }

    fun getPageText(position: Int): String {
        return pageTextCache[position] ?: ""
    }

    override fun getItemCount(): Int = chapters.size

    override fun createFragment(position: Int): Fragment {
        val chapter = chapters.getOrNull(position) ?: return DummyFragment()
        val text = chapter.title + "\n\n" + chapter.content
        pageTextCache[position] = text
        return ImmersivePageFragment.newInstance(text, fontSize, lineSpacing, isNightMode)
    }

    fun updateData(newChapters: List<ImmersiveChapter>) {
        chapters = newChapters
        pageTextCache.clear()
        notifyDataSetChanged()
    }
}
