package io.github.zakaria5729.phonetics.sample.xml

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import io.github.zakaria5729.phonetics.BanglaPhonetics
import io.github.zakaria5729.phonetics.EnglishPhonetics
import io.github.zakaria5729.phonetics.PhoneticMatchesResult
import io.github.zakaria5729.phonetics.Phonetics

class MainActivity : Activity() {
    private val bangla = BanglaPhonetics()
    private val english = EnglishPhonetics()

    private lateinit var input: EditText
    private lateinit var results: TextView
    private lateinit var languageGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        results = findViewById(R.id.results)
        languageGroup = findViewById(R.id.languageGroup)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(text: Editable?) = search()
        })
        languageGroup.setOnCheckedChangeListener { _, _ -> search() }
    }

    private fun currentPhonetics(): Phonetics =
        if (languageGroup.checkedRadioButtonId == R.id.english) english else bangla

    private fun search() {
        val text = input.text.toString()
        if (text.isBlank()) {
            results.setText(R.string.waiting)
            return
        }
        currentPhonetics().generateAsync(text, minScore = 70.0, maxResults = 20) { result ->
            results.text = describe(result)
        }
    }

    private fun describe(result: PhoneticMatchesResult): String {
        result.error?.let { return it.message }
        return result.matches.joinToString("\n") { match -> "${match.word}  ${match.score}%" }
    }

    override fun onDestroy() {
        bangla.dispose()
        english.dispose()
        super.onDestroy()
    }
}
