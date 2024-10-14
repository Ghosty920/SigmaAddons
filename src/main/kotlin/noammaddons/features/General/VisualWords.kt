package noammaddons.features.General

import noammaddons.utils.JsonUtils.fetchJsonWithRetry


object VisualWords {
    private var wordsMap: Map<String, String>? = null
	
    init {
	    fetchJsonWithRetry<Map<String, String>>(
		    "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/VisualWords.json"
		) { wordsMap = it }
    }

    /**
     * @see noammaddons.mixins.MixinFontRenderer
    */
    fun replaceText(text: String?): String? {
        if (text == null) return text
        if (wordsMap == null) return text
		//if (mc.currentScreen?.javaClass?.name == "gg.essential.vigilance.gui.settingsgui") return text
	    
        var newText = text
        for (actualText in wordsMap!!.keys) {
            newText = newText?.replace(actualText, wordsMap!![actualText]!!)
        }
        return newText
    }
}