@file:Suppress("NAME_SHADOWING")

import bean.AssistantStudent
import cc.moecraft.icq.PicqConfig
import cc.moecraft.icq.PicqBotX
import cc.moecraft.icq.sender.IcqHttpApi
import listen.AskForLeaveEventPrivateMessage
import tool.AssistantUtil
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.text.ParsePosition


val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")// 格式化时间
const val splitLine = "-------------------\n"


val 实验室助理群 = 436641186.toLong()
val 其他助理群 = 286199556.toLong()


fun main() {
    while (true) {

        try {
            // 创建机器人对象 ( 传入配置 )
            val bot = PicqBotX(PicqConfig(31092))
            // 添加一个机器人账户 ( 名字, 发送URL, 发送端口 )
            bot.addAccount("test", "127.0.0.1", 31091)
            Thread {
                while (true) {
                    val date = Date()// 获取当前时间
                    val time = dateFormat.format(date)
                    bot.accountManager.nonAccountSpecifiedApi.sendGroupMsg(451094615, "${time}\n\n正常运行")
                    Thread.sleep(1000 * 60 * 10)
                }
            }.start()

            实验室助理(bot, 实验室助理群)
            其他助理(bot, 其他助理群)

            bot.eventManager.registerListeners(
                    AskForLeaveEventPrivateMessage()
            )

            // 启动机器人, 不会占用主线程
            bot.startBot()
            Thread.sleep(Long.MAX_VALUE)
        } catch (e: Exception) {

        } finally {
            println("发生未知错误，自动重启")
            Thread.sleep(1000 * 5)
            continue
        }
    }
}

private fun 实验室助理(bot: PicqBotX, group: Long) {
    var sentTime: String = ""
    Thread {
        val icqHttpApi = bot.accountManager.nonAccountSpecifiedApi
        while (true) {
            val (time, dayForWeek, dayForTime) = 初始化当前数据()
            val turn: List<String>? = AssistantUtil.laboratoryWatchList["$dayForWeek$dayForTime".toInt()]
            if (turn == null) {
                sentTime = 无人值班处理(time, sentTime, icqHttpApi, group, dayForTime)
            } else {
                val t = time.substringAfter(" ").substringBeforeLast(":")
                if (sentTime != t) {
                    sentTime = t
                    val students = turn.map { AssistantUtil.assistantDateList[it] }
                    val stringBuilder = java.lang.StringBuilder()
                    stringBuilder.append("今天第${dayForTime}轮值班\n" +
                            "时间：${AssistantUtil.period(dayForTime)}\n")
                    值班人员位置和信息添加("实验室", stringBuilder, splitLine, students)
                    值班消息末尾(stringBuilder)
                    icqHttpApi.sendGroupMsg(group, stringBuilder.toString())
                    icqHttpApi.sendGroupMsg(451094615, stringBuilder.toString())//测试群

                    寻找此班次请假的处理(listOf("实验室"), time, dayForTime, icqHttpApi, group)
                }
            }

        }
    }.start()
}


private fun 其他助理(bot: PicqBotX, group: Long) {
    var sentTime: String = ""
    Thread {
        val icqHttpApi = bot.accountManager.nonAccountSpecifiedApi
        while (true) {
            val (time, dayForWeek, dayForTime) = 初始化当前数据()

            if (dayForTime == 0) {
                continue
            }
            val turnS = AssistantUtil.schoolRollWatchList["$dayForWeek$dayForTime".toInt()]
            val turnC = AssistantUtil.collegeWatchList["$dayForWeek$dayForTime".toInt()]
            val turnA = AssistantUtil.academicWorkerWatchList["$dayForWeek$dayForTime".toInt()]
            if (turnS == null && turnA == null && turnC == null) {
                sentTime = 无人值班处理(time, sentTime, icqHttpApi, group, dayForTime)
            } else {
                val t = time.substringAfter(" ").substringBeforeLast(":")
                if (sentTime != t) {
                    sentTime = t
                    val stringBuilder = StringBuilder()
                    stringBuilder.apply {
                        append("今天第${dayForTime}轮值班\n时间：${AssistantUtil.period(dayForTime)}\n")
                        append(splitLine)

                        值班人员位置和信息添加("学籍办", stringBuilder, splitLine, turnS?.map { AssistantUtil.assistantDateList[it] })

                        值班人员位置和信息添加("学工办", stringBuilder, splitLine, turnA?.map { AssistantUtil.assistantDateList[it] })

                        值班人员位置和信息添加("院办", stringBuilder, splitLine, turnC?.map { AssistantUtil.assistantDateList[it] })


                        值班消息末尾(stringBuilder)

                    }
                    icqHttpApi.sendGroupMsg(group, stringBuilder.toString())
                    icqHttpApi.sendGroupMsg(451094615, stringBuilder.toString())//测试群
                    寻找此班次请假的处理(listOf("学籍办", "学工办", "院办"), time, dayForTime, icqHttpApi, group)
                }
            }

        }

    }.start()
}


private fun 初始化当前数据(): Triple<String, Int, Int> {
    Thread.sleep(2000)
    val date = Date()// 获取当前时间
//    val time = "2019-09-24 10:00:00"//测试语句
    val time = dateFormat.format(date)
//    val dayForWeek = 2//测试语句
//    val dayForTime = 2//测试语句
    val dayForWeek = AssistantUtil.dayForWeek(time.substringBefore(" "))
    val dayForTime = AssistantUtil.dayForTime(time.substringAfter(" "))
    return Triple(time, dayForWeek, dayForTime)
}

private fun 无人值班处理(time: String, sentTime: String, icqHttpApi: IcqHttpApi, group: Long, dayForTime: Int): String {
    var sentTime1 = sentTime
    val t = time.substringAfter(" ").substringBeforeLast(":")
    if (sentTime1 != t) {
        sentTime1 = t
        val t = time.substringAfter(" ").substringBeforeLast(":")
        if (sentTime1 != t) {
            sentTime1 = t
            icqHttpApi.sendGroupMsg(group, "今天第${dayForTime}轮值班\n" +
                    "时间：${AssistantUtil.period(dayForTime)}\n" +
                    "此时间段无人值班")
            icqHttpApi.sendGroupMsg(451094615, "今天第${dayForTime}轮值班\n" +
                    "时间：${AssistantUtil.period(dayForTime)}\n" +
                    "此时间段无人值班")
        }
    }
    return sentTime1
}

private fun 寻找此班次请假的处理(nameList: List<String>, time: String, dayForTime: Int, icqHttpApi: IcqHttpApi, group: Long) {
    val stringBuilder2 = StringBuilder()
    AssistantUtil.leaveDataBean.leaveDatas.forEach { leaveDatasEntity ->
        val lTime = SimpleDateFormat("yyyy年MM月dd日").parse(leaveDatasEntity.content.substringBefore("第"), ParsePosition(0))?.time
                ?: 0
        val nTime = SimpleDateFormat("yyyy-MM-dd").parse(time.substringBefore(" "), ParsePosition(0))?.time
                ?: 0
        if (nTime == lTime) {
            if (digitalConversion(leaveDatasEntity.content.substringAfter("日第").substringBefore("节课"))
                    == dayForTime) {
                nameList.forEach AAA@{
                    if (leaveDatasEntity.type.substringBefore("助理") == it) {
                        stringBuilder2.append("【${leaveDatasEntity.name}】")
                        return@AAA
                    }
                }
            }
        }
    }
    if (stringBuilder2.isNotEmpty()) {
        stringBuilder2.append("有事请假，请各位老师注意")
        icqHttpApi.sendGroupMsg(group, stringBuilder2.toString())
        icqHttpApi.sendGroupMsg(451094615, stringBuilder2.toString())//测试群
    }
}

private fun 值班消息末尾(stringBuilder: java.lang.StringBuilder) {
    stringBuilder.append("请相应同学按时到达,如有事不能来请【提前】【提前】【提前】私聊机器人请假（重要的事说三遍）" +
            "向机器人发送任意私聊消息即可得到请假文本格式")
}

private fun 值班人员位置和信息添加(name: String, stringBuilder: java.lang.StringBuilder, splitLine: String, students: List<AssistantStudent?>?) {
    students?.let {
        stringBuilder.apply {
            append("【${name}】\n")
            students.forEach {
                it?.let {
                    append("${it.name}-[CQ:at,qq=${it.qqNumber}]\n联系电话：${it.phoneNumber}\n")
//                                append("${it.name}-[CQ:at,qq=1246634075]\n联系电话：${it.phoneNumber}\n")//测试语句
                }
            }
            append(splitLine)
        }
    }

}


fun digitalConversion(str: String): Int = when (str) {
    "12" -> 1
    "34" -> 2
    "56" -> 3
    "78" -> 4
    else -> 0
}

