package org.sddn.plugin.hibiki

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.MessageChain
import java.sql.Time
import kotlin.math.abs

object Alarm {
    fun addAlarmToSendMessage(timeInMillis: Long, notice: MessageChain, contact: Contact){
        GlobalScope.launch {
            while (true) {
                if (timeInMillis - System.currentTimeMillis() < 2000){
                    contact.sendMessage(notice)
                    break
                } else {
                    delay((timeInMillis - System.currentTimeMillis())/2)
                }
            }
        }
    }
}