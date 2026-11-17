package com.custom.naratgul

import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

class NaratgulImeService : InputMethodService() {

    private val automata = NaratgulAutomata()
    private lateinit var layoutKorean: LinearLayout
    private lateinit var layoutEnglish: LinearLayout
    private lateinit var layoutSymbol: LinearLayout

    private var isShift = false
    private var puncIndex = 0
    private val puncCycle = listOf(".", ",", "?", "!")

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)
        layoutKorean = root.findViewById(R.id.layoutKorean)
        layoutEnglish = root.findViewById(R.id.layoutEnglish)
        layoutSymbol = root.findViewById(R.id.layoutSymbol)

        setupKoreanKeys(root)
        setupEnglishKeys(root)
        setupSymbolKeys(root)

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        automata.reset()
        puncIndex = 0
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        automata.commitCurrent(currentInputConnection)
        super.onFinishInputView(finishingInput)
    }

    private fun vibrateTick() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(12)
        }
    }

    private fun switchMode(mode: String) {
        automata.commitCurrent(currentInputConnection)
        layoutKorean.visibility = if (mode == "KO") View.VISIBLE else View.GONE
        layoutEnglish.visibility = if (mode == "EN") View.VISIBLE else View.GONE
        layoutSymbol.visibility = if (mode == "SYM") View.VISIBLE else View.GONE
    }

    private fun setupKoreanKeys(root: View) {
        val consonants = mapOf(
            R.id.k_g to 'ㄱ',
            R.id.k_n to 'ㄴ',
            R.id.k_r to 'ㄹ',
            R.id.k_m to 'ㅁ',
            R.id.k_s to 'ㅅ',
            R.id.k_ng to 'ㅇ'
        )
        for ((id, ch) in consonants) {
            root.findViewById<Button>(id).setOnClickListener {
                vibrateTick()
                puncIndex = 0
                automata.inputConsonant(currentInputConnection, ch)
            }
        }

        root.findViewById<Button>(R.id.k_aeo).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.inputVowelToggle(currentInputConnection, 'ㅏ', 'ㅓ')
        }

        root.findViewById<Button>(R.id.k_ou).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.inputVowelToggle(currentInputConnection, 'ㅗ', 'ㅜ')
        }

        root.findViewById<Button>(R.id.k_i).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.inputSingleVowel(currentInputConnection, 'ㅣ')
        }

        root.findViewById<Button>(R.id.k_eu).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.inputSingleVowel(currentInputConnection, 'ㅡ')
        }

        root.findViewById<Button>(R.id.k_stroke).setOnClickListener {
            vibrateTick()
            automata.applyStroke(currentInputConnection)
        }

        root.findViewById<Button>(R.id.k_double).setOnClickListener {
            vibrateTick()
            automata.applyDouble(currentInputConnection)
        }

        root.findViewById<Button>(R.id.k_del).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.backspace(currentInputConnection)
        }

        root.findViewById<Button>(R.id.k_space).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.commitCurrent(currentInputConnection)
            currentInputConnection?.commitText(" ", 1)
        }

        root.findViewById<Button>(R.id.k_enter).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.commitCurrent(currentInputConnection)
            sendDefaultEditorAction(true)
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }

        root.findViewById<Button>(R.id.k_punc).setOnClickListener {
            vibrateTick()
            val ic = currentInputConnection ?: return@setOnClickListener
            automata.commitCurrent(ic)
            if (puncIndex > 0) {
                ic.deleteSurroundingText(1, 0)
            }
            val ch = puncCycle[puncIndex % puncCycle.size]
            ic.commitText(ch, 1)
            puncIndex++
        }

        root.findViewById<Button>(R.id.k_comma).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.commitCurrent(currentInputConnection)
            currentInputConnection?.commitText(",", 1)
        }

        root.findViewById<Button>(R.id.k_dot).setOnClickListener {
            vibrateTick()
            puncIndex = 0
            automata.commitCurrent(currentInputConnection)
            currentInputConnection?.commitText(".", 1)
        }

        root.findViewById<Button>(R.id.k_to_en).setOnClickListener { vibrateTick(); switchMode("EN") }
        root.findViewById<Button>(R.id.k_to_sym).setOnClickListener { vibrateTick(); switchMode("SYM") }
    }

    private fun setupEnglishKeys(root: View) {
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3 = listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")

        val enButtons = mutableListOf<Button>()

        fun populateRow(container: LinearLayout, keys: List<String>) {
            for (k in keys) {
                val btn = createKeyButton(k, k == "⇧" || k == "⌫", if (k == "⇧" || k == "⌫") 1.4f else 1f)
                if (k != "⇧" && k != "⌫") enButtons.add(btn)
                btn.setOnClickListener {
                    vibrateTick()
                    when (k) {
                        "⇧" -> {
                            isShift = !isShift
                            enButtons.forEach { b ->
                                b.text = if (isShift) b.text.toString().uppercase() else b.text.toString().lowercase()
                            }
                        }
                        "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
                        else -> {
                            val out = if (isShift) k.uppercase() else k.lowercase()
                            currentInputConnection?.commitText(out, 1)
                            if (isShift) {
                                isShift = false
                                enButtons.forEach { b -> b.text = b.text.toString().lowercase() }
                            }
                        }
                    }
                }
                container.addView(btn)
            }
        }

        populateRow(root.findViewById(R.id.enRow1), row1)
        populateRow(root.findViewById(R.id.enRow2), row2)
        populateRow(root.findViewById(R.id.enRow3), row3)

        root.findViewById<Button>(R.id.en_space).setOnClickListener { vibrateTick(); currentInputConnection?.commitText(" ", 1) }
        root.findViewById<Button>(R.id.en_dot).setOnClickListener { vibrateTick(); currentInputConnection?.commitText(".", 1) }
        root.findViewById<Button>(R.id.en_enter).setOnClickListener { vibrateTick(); sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
        root.findViewById<Button>(R.id.en_to_ko).setOnClickListener { vibrateTick(); switchMode("KO") }
        root.findViewById<Button>(R.id.en_to_sym).setOnClickListener { vibrateTick(); switchMode("SYM") }
    }

    private fun setupSymbolKeys(root: View) {
        val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
        val row3 = listOf("*", """, "'", ":", ";", "!", "?", "~", "⌫")

        fun populateSym(container: LinearLayout, keys: List<String>) {
            for (k in keys) {
                val btn = createKeyButton(k, k == "⌫", if (k == "⌫") 1.5f else 1f)
                btn.setOnClickListener {
                    vibrateTick()
                    if (k == "⌫") {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                    } else {
                        currentInputConnection?.commitText(k, 1)
                    }
                }
                container.addView(btn)
            }
        }

        populateSym(root.findViewById(R.id.symRow1), row1)
        populateSym(root.findViewById(R.id.symRow2), row2)
        populateSym(root.findViewById(R.id.symRow3), row3)

        root.findViewById<Button>(R.id.sym_space).setOnClickListener { vibrateTick(); currentInputConnection?.commitText(" ", 1) }
        root.findViewById<Button>(R.id.sym_enter).setOnClickListener { vibrateTick(); sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
        root.findViewById<Button>(R.id.sym_to_ko).setOnClickListener { vibrateTick(); switchMode("KO") }
        root.findViewById<Button>(R.id.sym_to_en).setOnClickListener { vibrateTick(); switchMode("EN") }
    }

    private fun createKeyButton(label: String, isFunc: Boolean, weight: Float): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(if (isFunc) R.drawable.key_func_bg else R.drawable.key_bg)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                setMargins(3, 3, 3, 3)
            }
        }
    }
}
