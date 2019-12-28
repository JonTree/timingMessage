import cc.moecraft.icq.PicqBotX
import cc.moecraft.icq.PicqConfig
import cc.moecraft.icq.sender.IcqHttpApi
import listen.AskForLeaveEventPrivateMessage
import tool.AssistantUtil
import tool.SqlUtil
import java.text.SimpleDateFormat
import java.util.*

const val splitLine = "-------------------\n"

val 实验室助理群 = 436641186.toLong()
val 其他助理群 = 286199556.toLong()
val 寝室群 = 859805886.toLong()

fun main() {
    // 创建机器人对象 ( 传入配置 )
    val bot = PicqBotX(PicqConfig(31092))
    // 添加一个机器人账户 ( 名字, 发送URL, 发送端口 )
    bot.addAccount("test", "127.0.0.1", 5700)
    laboratoryAssistant(bot, 实验室助理群)
    otherAssistant(bot, 其他助理群)
    bot.eventManager.registerListeners(
            AskForLeaveEventPrivateMessage()
    )
    // 启动机器人, 不会占用主线程
    bot.config.isNoVerify = true
    bot.startBot()
}


/**
 * 初始化当前数据
 */
private fun initializeCurrentData(): Triple<String, Int, Int> {
    val date = Date()// 获取当前时间
    val time = "2019-12-27 07:45:00"//测试语句
//    val time = dateFormat.format(date)
//    val dayForWeek = 2//测试语句
//    val dayForTime = 2//测试语句
    val dayForWeek = AssistantUtil.dayForWeek(time.substringBefore(" "))
    val dayForTime = AssistantUtil.dayForTime(time.substringAfter(" "))
    return Triple(time, dayForWeek, dayForTime)
}

/**
 * 实验室助理
 */
private fun laboratoryAssistant(bot: PicqBotX, group: Long) {
    var sentTime = ""
    Thread {
        val icqHttpApi = bot.accountManager.nonAccountSpecifiedApi
        val sqlUtil = SqlUtil()
        while (true) {
            val (time, dayForWeek, dayForTime) = initializeCurrentData()
            val resultSet = sqlUtil.statement.executeQuery("select * from watchList where weekDay = $dayForWeek and timeTurn = $dayForTime and type = '实验室助理';")
            if (!resultSet.first()) {
                sentTime = unattendedProcessing(time, sentTime, icqHttpApi, group, dayForTime)
            } else {
                val t = time.substringAfter(" ").substringBeforeLast(":")
                if (sentTime != t) {
                    sentTime = t
                    val stringBuilder = java.lang.StringBuilder()
                    stringBuilder.append("今天第${dayForTime}轮值班\n" +
                            "时间：${AssistantUtil.period(dayForTime)}\n")
                    onDutyStaffLocationAndInformationAdded("实验室", stringBuilder, splitLine,dayForWeek,dayForTime,sqlUtil)
                    atTheEndOfTheDutyMessage(stringBuilder)
                    icqHttpApi.sendGroupMsg(group, stringBuilder.toString())
                    icqHttpApi.sendGroupMsg(group, "请值班的同学注意，到岗后，你们优先将没有上课的教室的讲台整理一下，将讲台桌面和座椅灰尘打扫一下，并将物品摆放整齐，谢谢！\n")
                    icqHttpApi.sendGroupMsg(451094615, stringBuilder.toString())//测试群
                    icqHttpApi.sendGroupMsg(451094615, "请值班的同学注意，每次值班时，请优先将没有上课的教室的讲台整理一下，将讲台桌面上的灰尘和教师座椅的灰尘打扫一下，并将物品摆放整齐，谢谢！\n")//测试群
                    lookingForTheTreatmentOfThisShift(listOf("实验室"), time, dayForTime, icqHttpApi, group,sqlUtil)
                }
            }
        }
    }.start()
}

/**
 * 其他助理
 */
private fun otherAssistant(bot: PicqBotX, group: Long) {
    var sentTime = ""
    Thread {
        val sqlUtil = SqlUtil()
        val icqHttpApi = bot.accountManager.nonAccountSpecifiedApi
        while (true) {
            val (time, dayForWeek, dayForTime) = initializeCurrentData()
            if (dayForTime == 0) {
                continue
            }
            val resultSet = sqlUtil.statement.executeQuery("select * from watchList where weekDay = $dayForWeek and timeTurn = $dayForTime and type in ('院办助理','学籍办助理','学工办助理');")
            if (!resultSet.first()) {
                sentTime = unattendedProcessing(time, sentTime, icqHttpApi, group, dayForTime)
            } else {
                val t = time.substringAfter(" ").substringBeforeLast(":")
                if (sentTime != t) {
                    sentTime = t
                    val stringBuilder = StringBuilder()
                    stringBuilder.apply {
                        append("今天第${dayForTime}轮值班\n时间：${AssistantUtil.period(dayForTime)}\n")
                        append(splitLine)
                        onDutyStaffLocationAndInformationAdded("学籍办", stringBuilder, splitLine, dayForWeek,dayForTime,sqlUtil)
                        onDutyStaffLocationAndInformationAdded("学工办", stringBuilder, splitLine, dayForWeek,dayForTime,sqlUtil)
                        onDutyStaffLocationAndInformationAdded("院办", stringBuilder, splitLine,dayForWeek,dayForTime,sqlUtil)
                        atTheEndOfTheDutyMessage(stringBuilder)
                    }
                    icqHttpApi.sendGroupMsg(group, stringBuilder.toString())
                    icqHttpApi.sendGroupMsg(451094615, stringBuilder.toString())//测试群
                    lookingForTheTreatmentOfThisShift(listOf("学籍办", "学工办", "院办"), time, dayForTime, icqHttpApi, group,sqlUtil)
                }
            }
        }
    }.start()
}


/**
 * 无人值班处理
 */
private fun unattendedProcessing(time: String, sentTime: String, icqHttpApi: IcqHttpApi, group: Long, dayForTime: Int): String {
    var sentTime1 = sentTime
    val t = time.substringAfter(" ").substringBeforeLast(":")
    if (sentTime1 != t) {
        sentTime1 = t
        val t1 = time.substringAfter(" ").substringBeforeLast(":")
        if (sentTime1 != t1) {
            sentTime1 = t1
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
private fun lookingForTheTreatmentOfThisShift(nameList: List<String>, time: String, dayForTime: Int, icqHttpApi: IcqHttpApi, group: Long, sqlUtil: SqlUtil) {
    val data = "${SimpleDateFormat("yyyy年MM月dd日").format(Date())}第${digitalNumConversion(dayForTime)}节课"
    var sql = "select * from leaveData where leaveData.leaveTime = '${data}' and leaveData.type in ("
    nameList.forEach {
        sql += "'${it}助理'"
        if (nameList.indexOf(it) != nameList.lastIndex) {
            sql += ","
        }
    }
    sql += ");"
    val resultSet = sqlUtil.statement.executeQuery(sql)
    val stringBuilder2 = StringBuilder()
    while (resultSet.next()) {
        stringBuilder2.append("【${resultSet.getString("name")}】")
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
            "向机器人发送任意私聊消息即可得到请假文本格式，若未能提前请假的将不能在群里通知，请各位老师自行在【http://139.196.143.240/leave/record/#/】查看")
}

/**
 * 值班人员位置和信息添加
 */
private fun onDutyStaffLocationAndInformationAdded(name: String, stringBuilder: StringBuilder, splitLine: String, dayForWeek: Int, dayForTime: Int, sqlUtil: SqlUtil) {
    val resultSet = sqlUtil.statement.executeQuery("select assistantstudent.* from watchList,assistantstudent where watchList.weekDay = $dayForWeek and watchList.timeTurn = $dayForTime and watchList.type = '${name}助理' and assistantstudent.name = watchList.name; ")
    stringBuilder.append("【${name}】\n")
    while (resultSet.next()) {
        val studentName = resultSet.getString("name")
        val studentPhoneNumber = resultSet.getString("phoneNumber")
        val studentQQNum = resultSet.getString("qqNumber")
        stringBuilder.append("${studentName}-[CQ:at,qq=${studentQQNum}]\n联系电话：${studentPhoneNumber}\n")
    }
    stringBuilder.append(splitLine)
}


fun digitalConversion(str: String): Int = when (str) {
    "12" -> 1
    "34" -> 2
    "56" -> 3
    "78" -> 4
    else -> 0
}

fun digitalNumConversion(num: Int): String = when (num) {
    1 -> "12"
    2 -> "34"
    3 -> "56"
    4 -> "78"
    else -> "00"
}

