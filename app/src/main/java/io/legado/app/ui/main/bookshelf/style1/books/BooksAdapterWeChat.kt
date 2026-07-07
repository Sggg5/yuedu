package io.legado.app.ui.main.bookshelf.style1.books

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemBookshelfWechatBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import splitties.views.onLongClick

class BooksAdapterWeChat(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<ViewBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ViewBinding {
        return ItemBookshelfWechatBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ViewBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        val wechatBinding = binding as ItemBookshelfWechatBinding
        if (payloads.isEmpty()) {
            wechatBinding.apply {
                tvName.text = item.name
                tvChapter.text = item.durChapterTitle.ifEmpty { item.latestChapterTitle }
                ivCover.load(item, false)

                // Calculate reading progress
                val progress = calculateProgress(item)
                progressBar.progress = progress
                tvProgress.text = "${progress}%"

                // Continue reading button
                btnContinueRead.setOnClickListener {
                    callBack.open(item)
                }
            }
        } else {
            for (i in payloads.indices) {
                val bundle = payloads[i] as Bundle
                bundle.keySet().forEach { key ->
                    when (key) {
                        "name" -> wechatBinding.tvName.text = item.name
                        "cover" -> wechatBinding.ivCover.load(item, false)
                        "refresh" -> {
                            wechatBinding.tvChapter.text = item.durChapterTitle.ifEmpty { item.latestChapterTitle }
                            val progress = calculateProgress(item)
                            wechatBinding.progressBar.progress = progress
                            wechatBinding.tvProgress.text = "${progress}%"
                        }
                    }
                }
            }
        }
    }

    private fun calculateProgress(book: Book): Int {
        val total = book.totalChapterNum
        val current = book.durChapterIndex
        return if (total > 0) {
            ((current.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ViewBinding) {
        val wechatBinding = binding as ItemBookshelfWechatBinding
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.open(it)
                }
            }
            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openBookInfo(it)
                }
            }
        }
        // The continue button already has its own click listener in convert()
    }
}
