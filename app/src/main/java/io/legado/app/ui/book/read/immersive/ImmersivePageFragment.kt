package io.legado.app.ui.book.read.immersive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.legado.app.R

class ImmersivePageFragment : Fragment() {

    companion object {
        private const val ARG_TEXT = "text"
        private const val ARG_FONT_SIZE = "fontSize"
        private const val ARG_LINE_SPACING = "lineSpacing"
        private const val ARG_NIGHT_MODE = "nightMode"

        fun newInstance(text: String, fontSize: Float, lineSpacing: Float, isNight: Boolean): ImmersivePageFragment {
            return ImmersivePageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                    putFloat(ARG_FONT_SIZE, fontSize)
                    putFloat(ARG_LINE_SPACING, lineSpacing)
                    putBoolean(ARG_NIGHT_MODE, isNight)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_immersive_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.tv_content)
        arguments?.let { args ->
            tv?.text = args.getString(ARG_TEXT, "")
            tv?.textSize = args.getFloat(ARG_FONT_SIZE, 18f)
            tv?.setLineSpacing(args.getFloat(ARG_LINE_SPACING, 8f), 1.0f)
            if (args.getBoolean(ARG_NIGHT_MODE, false)) {
                tv?.setTextColor(resources.getColor(R.color.read_text_night, null))
            } else {
                tv?.setTextColor(resources.getColor(R.color.read_text, null))
            }
        }
    }
}
