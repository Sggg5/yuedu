package io.legado.app.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefString
import io.legado.app.databinding.DialogAiBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch

class AIDialog : DialogFragment() {

    private val binding by viewBinding(DialogAiBinding::bind)
    private var currentText: String = ""

    companion object {
        private const val ARG_TEXT = "text"

        fun show(manager: androidx.fragment.app.FragmentManager, text: String) {
            val dialog = AIDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                }
            }
            dialog.show(manager, "AIDialog")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog)
        currentText = arguments?.getString(ARG_TEXT, "") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load API key from preferences
        val apiKey = requireContext().getPrefString(PreferKey.deepseekApiKey)
        if (!apiKey.isNullOrBlank()) {
            DeepSeekClient.updateConfig(DeepSeekClient.getCurrentConfig().copy(apiKey = apiKey))
        }

        binding.btnSummarize.setOnClickListener {
            callAI(DeepSeekClient.Prompts.summarize(currentText))
        }
        binding.btnExplain.setOnClickListener {
            callAI(DeepSeekClient.Prompts.explain(currentText))
        }
        binding.btnQa.setOnClickListener {
            val question = binding.etQuestion.text.toString().trim()
            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "请输入问题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            callAI(DeepSeekClient.Prompts.qa(currentText, question))
        }
    }

    private fun callAI(messages: List<DeepSeekClient.ChatMessage>) {
        binding.tvResult.text = getString(R.string.ai_loading)
        lifecycleScope.launch {
            val result = DeepSeekClient.chat(messages)
            result.onSuccess { response ->
                binding.tvResult.text = response
            }.onFailure { error ->
                binding.tvResult.text = error.message ?: getString(R.string.ai_error)
            }
        }
    }
}
