package me.lemonhall.worddragon.data.content

import android.content.Context
import me.lemonhall.worddragon.domain.game.IdiomDefinition
import org.json.JSONObject

interface IdiomCatalogDataSource {
    fun getIdiom(id: String): IdiomDefinition

    fun getIdioms(ids: List<String>): List<IdiomDefinition>
}

class AssetIdiomCatalogDataSource(
    private val context: Context,
) : IdiomCatalogDataSource {
    private val idiomsById by lazy(LazyThreadSafetyMode.NONE) { loadIdiomsById() }

    override fun getIdiom(id: String): IdiomDefinition =
        requireNotNull(idiomsById[id]) { "Missing idiom: $id" }

    override fun getIdioms(ids: List<String>): List<IdiomDefinition> = ids.map(::getIdiom)

    private fun loadIdiomsById(): Map<String, IdiomDefinition> {
        val payload = context.assets.open(CATALOG_ASSET_PATH).bufferedReader().use { it.readText() }
        val entries = JSONObject(payload).getJSONArray("entries")
        return buildMap(entries.length()) {
            repeat(entries.length()) { index ->
                val entry = entries.getJSONObject(index)
                val idiom =
                    IdiomDefinition(
                        id = entry.getString("id"),
                        text = entry.getString("text"),
                        shortExplanation = entry.optString("short_explanation"),
                        ttsText = entry.optString("tts_text"),
                        pinyin = entry.optString("pinyin"),
                    )
                put(idiom.id, idiom)
            }
        }
    }

    private companion object {
        const val CATALOG_ASSET_PATH = "content/idiom_catalog.json"
    }
}
