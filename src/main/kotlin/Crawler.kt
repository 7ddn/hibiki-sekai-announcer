package org.sddn.plugin.hibiki

import com.alibaba.fastjson.*
import kotlinx.coroutines.delay
import org.sddn.plugin.hibiki.beans.Card
import java.io.File


object Crawler {
    suspend fun cardCrawler(maxID : Int): Int {
        var cardID = PluginData.maxCardId
        while (true) {
            if (cardID > maxID) return 0
            cardID++
            try {
                val cardUrl = PluginConfig.APIs["card"] + OtherUtils.intTo3Word(cardID)
                val response = HttpUtils.httpGet(cardUrl)
                if (response == null || !response.isSuccessful) {
                    // cardID++
                    continue
                }
                val card = JSON.parseObject(response.body!!.string())
                val count = card.getString("total").toInt()
                if (count == 0) {
                    /* 由于不明原因(废案?) 初期卡各自留了1个四星卡的空位，因此108号之前的卡片并不是连号的
                    * 故此处对编号进行特判，如果在108号之前可以认为并未加载完全部新卡
                     */
                    if (cardID <= 108) {
                        PluginData.cards.add(Card(id = cardID, cardName = "unavailable"))
                        continue
                    } else {
                        PluginData.maxCardId = cardID - 1
                        PluginMain.logger.info("读取到最新的卡牌ID为${cardID - 1}")
                        break
                    }

                } else {
                    val data = card.getJSONArray("data").getJSONObject(0)
                    // PluginMain.logger.info(data.toString())
                    val id = data.getIntValue("id")
                    val characterID = data.getIntValue("characterId")
                    val cardName = data.getString("prefix")
                    val cardSkillName = data.getString("cardSkillName")
                    val attr = data.getString("attr")
                    val rarity = data.getIntValue("rarity")
                    val cardSkillID = data.getIntValue("skillId")
                    val relativeID = data.getString("assetbundleName").substring(9).toInt()

                    val newCard = Card(
                        id = id,
                        characterID = characterID,
                        cardName = cardName,
                        cardSkillName = cardSkillName,
                        attr = attr,
                        rarity = rarity,
                        cardSkillID = cardSkillID,
                        relativeID = relativeID
                    )

                    PluginData.cards.add(newCard)
                    if (PluginData.chara2card[characterID] == null) PluginData.chara2card[characterID] = mutableSetOf()
                    PluginData.chara2card[characterID]!!.add(newCard.id)
                    PluginMain.logger.info("添加新卡片 id = $id")
                    response.close()
                    delay(500L)
                }


                // PluginData.cards.

            } catch (e: Exception) {
                PluginMain.logger.info(e.message)
                return -1
            }
        }
        return 0
    }

    suspend fun cardPicCrawler(ifCardOk: Int) {
        OtherUtils.checkAndCreateWorkingDir("${PluginConfig.WorkingDir}pic\\normal\\")
        OtherUtils.checkAndCreateWorkingDir("${PluginConfig.WorkingDir}pic\\trained\\")

        val cards = PluginData.cards

        /*测试用
        cards.clear()
        cards.add(Card(
            characterID = 20,
            relativeID = 8,
            rarity = 4
        ))
        测试用结束*/

        cards.forEach {
            if (it.cardName == "unavailable") return@forEach; // 同上 前108号中有缺失的部分
            if (it.ifNormalCached && (it.rarity <= 2 || it.ifTrainedCached)) return@forEach
            val characterID = OtherUtils.intTo3Word(it.characterID)
            val relativeID = OtherUtils.intTo3Word(it.relativeID)

            try {
                val urlNormal = HttpUtils.cardNormalUrlGenerate(characterID, relativeID)
                val file = File("${PluginConfig.WorkingDir}pic\\normal\\${it.id}_normal.png")
                HttpUtils.getImageFromUrlOrSave(file, urlNormal)
                if (file.canRead()) {
                    it.ifNormalCached = true
                    PluginMain.logger.info("添加新特训前卡图 id = ${it.id}")
                }
                if (it.rarity > 2) {
                    val urlAfterTrained = HttpUtils.cardAfterTrainingUrlGenerate(characterID, relativeID)
                    val file = File("${PluginConfig.WorkingDir}pic\\trained\\${it.id}_trained.png")
                    HttpUtils.getImageFromUrlOrSave(file, urlAfterTrained)
                    if (file.canRead()) {
                        it.ifTrainedCached = true
                        PluginMain.logger.info("添加新特训后卡图 id = ${it.id}")
                    }
                }
            } catch (e: Exception) {
                PluginMain.logger.info(e.message)
            }
        }

    }


}