@file:Suppress("NAME_SHADOWING")

import bean.AssistantStudent
import bean.CountData
import bean.EntertainmentBanBean
import cc.moecraft.icq.PicqConfig
import cc.moecraft.icq.PicqBotX
import cc.moecraft.icq.sender.IcqHttpApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import listen.*
import tool.AssistantUtil
import tool.MFile
import java.text.SimpleDateFormat
import java.util.*
import java.text.ParsePosition
import javax.xml.crypto.Data


val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")// 格式化时间
const val splitLine = "-------------------\n"


val 实验室助理群 = 436641186.toLong()
val 其他助理群 = 286199556.toLong()

val currentDutyPersons = mutableListOf<String>()


fun main() {
    // 创建机器人对象 ( 传入配置 )
    val bot = PicqBotX(PicqConfig(31092))
    // 添加一个机器人账户 ( 名字, 发送URL, 发送端口 )
    bot.addAccount("test", "127.0.0.1", 5700)
    Thread {
        while (true) {
            val date = Date()// 获取当前时间
            val time = dateFormat.format(date)
            bot.accountManager.nonAccountSpecifiedApi.sendGroupMsg(451094615, "${time}\n\n正常运行")
            Thread.sleep(1000 * 60 * 10)
        }
    }.start()
    if (MFile.isExsit("Data/禁言关键词.txt")) {
        forbiddenKeywordList = Gson().fromJson(MFile.read("Data/禁言关键词.txt"), object : TypeToken<MutableList<String>>() {}.type)
    }
    if (MFile.isExsit("Data/娱乐禁言排行榜.txt")) {
        entertainmentMessageRankingList = Gson().fromJson(MFile.read("Data/娱乐禁言排行榜.txt"), object : TypeToken<LinkedList<EntertainmentBanBean>>() {}.type)
    }
    if (MFile.isExsit("Data/复读条数设置.txt")) {
        val countData: CountData = Gson().fromJson(MFile.read("Data/复读条数设置.txt"), CountData::class.java)
        badRepeatCount = countData.badRepeatCount
        repeatCount = countData.repeatCount
    }
    laboratoryAssistant(bot, 实验室助理群)
    otherAssistant(bot, 其他助理群)
    bot.eventManager.registerListeners(
            AskForLeaveEventPrivateMessage()
    )

    // 启动机器人, 不会占用主线程
    bot.config.isNoVerify = true
    bot.startBot()
    Thread {
        while (true) {
            Thread.sleep(1000)
            if (MFile.isExsit("Data/防止意外退出重启.txt")) {
                if (Date().time - MFile.read("Data/防止意外退出重启.txt").toLong() > 2 * 60 * 1000) {
                    main()
                    return@Thread
                }else{
                    防止意外退出()
                }
            } else {
                MFile.createFolder("Data/防止意外退出重启.txt", false)
                防止意外退出()
            }
        }
    }
}


/**
 * 实验室助理
 */
private fun laboratoryAssistant(bot: PicqBotX, group: Long) {
    var sentTime: String = ""
    val dutyPersons = mutableListOf<String>()
    Thread {
        val icqHttpApi = bot.accountManager.nonAccountSpecifiedApi
        while (true) {
            防止意外退出()
            val (time, dayForWeek, dayForTime) = initializeCurrentData()
            val turn: List<String>? = AssistantUtil.laboratoryWatchList["$dayForWeek$dayForTime".toInt()]
            if (turn == null) {
                sentTime = unattendedProcessing(time, sentTime, icqHttpApi, group, dayForTime)
            } else {
                val t = time.substringAfter(" ").substringBeforeLast(":")
                if (sentTime != t) {
                    sentTime = t
                    val students = turn.map { AssistantUtil.assistantDateList[it] }
                    for (person in dutyPersons) {
                        currentDutyPersons.remove(person)
                    }
                    dutyPersons.clear()
                    for (student in students) {
                        student?.let {
                            dutyPersons.add(student.name)
                            currentDutyPersons.add(student.name)
                        }
                    }
                    val stringBuilder = java.lang.StringBuilder()
                    stringBuilder.append("今天第${dayForTime}轮值班\n" +
                            "时间：${AssistantUtil.period(dayForTime)}\n")
                    onDutyStaffLocationAndInformationAdded("实验室", stringBuilder, splitLine, students)
                    atTheEndOfTheDutyMessage(stringBuilder)
                    icqHttpApi.sendGroupMsg(group, stringBuilder.toString())
                    icqHttpApi.sendGroupMsg(group, "请值班的同学注意，到岗后，你们优先将没有上课的教室的讲台整理一下，将讲台桌面和座椅灰尘打扫一下，并将物品摆放整齐，谢谢！\n")
                    icqHttpApi.sendGroupMsg(451094615, stringBuilder.toString())//测试群
                    icqHttpApi.sendGroupMsg(451094615, "请值班的同学注意，每次值班时，请优先将没有上课的教室的讲台整理一下，将讲台桌面上的灰尘和教师座椅的灰尘打扫一下，并将物品摆放整齐，谢谢！\n")//测试群
                    lookingForTheTreatmentOfThisShift(listOf("实验室"), time, dayForTime, icqHttpApi, group)
                }
            }

        }
    }.start()
}

private fun 防止意外退出() {
    MFile.saveToFile("Data/防止意外退出重启.txt", "" + Date().time)
}

/**
 * 其他助理
 */
private fun otherAssistant(bot: PicqBotX, group: Long) {
    var sentTime: String = ""
    val dutyPersons = mutableListOf<String>()
    Thread {
        val icqHttpApi = bot.accountManager.nonAccountSpecifiedApi
        while (true) {
            防止意外退出()
            val (time, dayForWeek, dayForTime) = initializeCurrentData()

            if (dayForTime == 0) {
                continue
            }
            val turnS = AssistantUtil.schoolRollWatchList["$dayForWeek$dayForTime".toInt()]
            val turnC = AssistantUtil.collegeWatchList["$dayForWeek$dayForTime".toInt()]
            val turnA = AssistantUtil.academicWorkerWatchList["$dayForWeek$dayForTime".toInt()]
            if (turnS == null && turnA == null && turnC == null) {
                sentTime = unattendedProcessing(time, sentTime, icqHttpApi, group, dayForTime)
            } else {
                val t = time.substringAfter(" ").substringBeforeLast(":")
                if (sentTime != t) {
                    sentTime = t
                    val stringBuilder = StringBuilder()
                    stringBuilder.apply {
                        append("今天第${dayForTime}轮值班\n时间：${AssistantUtil.period(dayForTime)}\n")
                        append(splitLine)
                        val students = LinkedList<AssistantStudent?>()

                        onDutyStaffLocationAndInformationAdded("学籍办", stringBuilder, splitLine, turnS?.map { AssistantUtil.assistantDateList[it] }?.apply { students.addAll(this) })

                        onDutyStaffLocationAndInformationAdded("学工办", stringBuilder, splitLine, turnA?.map { AssistantUtil.assistantDateList[it] }?.apply { students.addAll(this) })

                        onDutyStaffLocationAndInformationAdded("院办", stringBuilder, splitLine, turnC?.map { AssistantUtil.assistantDateList[it] }?.apply { students.addAll(this) })

                        for (person in dutyPersons) {
                            currentDutyPersons.remove(person)
                        }
                        dutyPersons.clear()
                        for (student in students) {
                            student?.let {
                                dutyPersons.add(student.name)
                                currentDutyPersons.add(student.name)
                            }
                        }
                        atTheEndOfTheDutyMessage(stringBuilder)

                    }
                    icqHttpApi.sendGroupMsg(group, stringBuilder.toString())
                    icqHttpApi.sendGroupMsg(451094615, stringBuilder.toString())//测试群
                    lookingForTheTreatmentOfThisShift(listOf("学籍办", "学工办", "院办"), time, dayForTime, icqHttpApi, group)
                }
            }
        }

    }.start()
}


/**
 * 初始化当前数据
 */
private fun initializeCurrentData(): Triple<String, Int, Int> {
    Thread.sleep(2000)
    val date = Date()// 获取当前时间
//    val time = "2019-10-22 13:45:00"//测试语句
    val time = dateFormat.format(date)
//    val dayForWeek = 2//测试语句
//    val dayForTime = 2//测试语句
    val dayForWeek = AssistantUtil.dayForWeek(time.substringBefore(" "))
    val dayForTime = AssistantUtil.dayForTime(time.substringAfter(" "))
    return Triple(time, dayForWeek, dayForTime)
}

/**
 * 无人值班处理
 */
private fun unattendedProcessing(time: String, sentTime: String, icqHttpApi: IcqHttpApi, group: Long, dayForTime: Int): String {
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

/**
 * 寻找此班次请假的处理
 */
private fun lookingForTheTreatmentOfThisShift(nameList: List<String>, time: String, dayForTime: Int, icqHttpApi: IcqHttpApi, group: Long) {
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

/**
 * 值班消息末尾
 */
private fun atTheEndOfTheDutyMessage(stringBuilder: java.lang.StringBuilder) {
    stringBuilder.append("请相应同学按时到达,如有事不能来请【提前】【提前】【提前】私聊机器人请假（重要的事说三遍）" +
            "向机器人发送任意私聊消息即可得到请假文本格式")
}

/**
 * 值班人员位置和信息添加
 */
private fun onDutyStaffLocationAndInformationAdded(name: String, stringBuilder: java.lang.StringBuilder, splitLine: String, students: List<AssistantStudent?>?) {
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

