package com.iconchanger.rollingicons.utils

import com.iconchanger.rollingicons.R
import com.mobi.libraryads.data.LanguageModel

enum class EnumSelectLanguage(val id: Int, val nameLanguage: Int, val flag: Int, val code: String) {
    UNITED_STATES(1, R.string.language_english, R.drawable.flag_us, "en"),
    FRENCH(2, R.string.language_french, R.drawable.flag_fr, "fr"),
    GERMAN(3, R.string.language_german, R.drawable.flag_de, "de"),
    HINDI(4, R.string.language_hindi, R.drawable.flag_in, "hi"),
    INDONESIAN(5, R.string.language_indonesian, R.drawable.flag_id, "id"),
    PORTUGUESE(6, R.string.language_portuguese, R.drawable.flag_pt, "pt"),
    SPANISH(7, R.string.language_spanish, R.drawable.flag_es, "es"),
    VIETNAMESE(8, R.string.language_vietnamese, R.drawable.flag_vn, "vi"),
    JAPANESE(9, R.string.language_japanese, R.drawable.flag_jp, "ja");

    companion object {
        fun toLanguageModelList(): ArrayList<LanguageModel> {
            val list = arrayListOf<LanguageModel>()
            for (lang in entries) {
                list.add(
                    LanguageModel(
                        txtLanguage = lang.nameLanguage,
                        icFlag = lang.flag,
                        code = lang.code,
                        isSelect = false
                    )
                )
            }
            return list
        }
    }
}
