package listen

import bean.*
import tool.AssistantUtil
import tool.FileUtils
import tool.ThreadPoolUtils
import cc.moecraft.icq.event.EventHandler
import cc.moecraft.icq.event.IcqListener
import cc.moecraft.icq.event.events.message.EventPrivateMessage
import com.google.gson.Gson
import tool.FileUtils.createExcel
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap
import cc.moecraft.icq.event.events.message.EventGroupMessage
import tool.MFile
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import java.net.URLEncoder
import kotlin.random.asKotlinRandom


var forbiddenKeywordList = mutableListOf<String>()
var entertainmentMessageRankingList = LinkedList<娱乐禁言Bean>()

var repeatCount = 6
var badRepeatCount = 4

class AskForLeaveEventPrivateMessage : IcqListener() {

    private val leaveQueue = HashMap<String, LeaveQueueMessage>()

    //关键词添加消息队列
    private val keywordsAddMessageQueue = HashMap<Long, KeywordMessage>()


    val 刘星的QQ = 1623044256.toLong()
    val 刘旭的QQ = 2108372936.toLong()
    val 我的qq = 1246634075.toLong()

    //关键字添加者
    val keywordAdder = listOf(
            1044805408.toLong(),
            1142013327.toLong(),
            897598260.toLong(),
            1246634075.toLong(),
            619750063.toLong(),
            815024038.toLong(),
            844742916.toLong()
    )


    @EventHandler
    fun saveTheLeaveEvent(eventPrivateMessage: EventPrivateMessage) {
        //是否是关键字添加者
        var isItAKeywordAdder = false
        for (i in keywordAdder) {
            isItAKeywordAdder = (eventPrivateMessage.senderId == i)
            if (isItAKeywordAdder) {
                break
            }
        }
        if (isItAKeywordAdder) {
            keywordAdderMessageProcessing(eventPrivateMessage)
        } else {
            when (eventPrivateMessage.message) {
                "请假文档" -> processingDocumentRequestMessage(eventPrivateMessage)
                "补登请假" -> handlingTheLeaveRequestText(eventPrivateMessage)
                else -> leaveProcess(eventPrivateMessage)
            }
        }
    }


    //娱乐复读消息收集
    val repeatBanList = LinkedList<RepeatData>()

    //恶劣复读消息收集
    val badSingleRepeat = LinkedList<RepeatData>()
        val group = 451094615.toLong()//哒哒群
//    val group = 483100546.toLong()//安卓群
    //    val group = 913874135.toLong()//测试群

    //关键词禁言
    @EventHandler
    fun keyWordsBan(eventGroupMessage: EventGroupMessage) {
        if (eventGroupMessage.groupId == group) {
            if (娱乐禁言条数更改命令(eventGroupMessage)) return
            if (恶劣复读条数更改(eventGroupMessage)) return
            if (功能命令处理(eventGroupMessage)) return
            //搜索引擎
            if (searchEngine(eventGroupMessage)) return

            if (封神榜命令(eventGroupMessage)) return
            //恶劣禁言
//            if (恶劣禁言处理(eventGroupMessage)) return

            娱乐禁言处理(eventGroupMessage)
            //禁言关键字List
            敏感关键字处理(eventGroupMessage)
        }
    }

    private fun 恶劣禁言处理(eventGroupMessage: EventGroupMessage): Boolean {
        badSingleRepeat.add(RepeatData(eventGroupMessage.senderId, eventGroupMessage.message))
        if (badSingleRepeat.size > 1) {
            return if (badSingleRepeat[0].qqnum == badSingleRepeat.last.qqnum && badSingleRepeat[0].str == badSingleRepeat.last.str) {
                if (badSingleRepeat.size >= badRepeatCount) {
                    eventGroupMessage.httpApi.setGroupBan(group, badSingleRepeat[0].qqnum, 60 * 10)
                    eventGroupMessage.httpApi.sendGroupMsg(group, "[CQ:at,qq=${badSingleRepeat[0].qqnum}]恶劣复读")
                    badSingleRepeat.clear()
                }
                true
            } else {
                badSingleRepeat.clear()
                false
            }
        }
        return false
    }

    private fun 恶劣复读条数更改(eventGroupMessage: EventGroupMessage): Boolean {
        val p = Pattern.compile("#恶劣复读禁言条数.")
        val m = p.matcher(eventGroupMessage.message)
        if (m.lookingAt()) {
            if (eventGroupMessage.isAdmin(eventGroupMessage.senderId)) {
                val str = eventGroupMessage.message.substringAfter("#恶劣复读禁言条数").substring(1)
                val mp = Pattern.compile("\\d+")
                val mm = mp.matcher(str)
                if (mm.find()) {
                    val count = mm.group(0).toInt()
                    if (count in 4..59) {
                        badRepeatCount = count
                        eventGroupMessage.respond("设置成功:$badRepeatCount")
                        MFile.saveToFile("Data/复读条数设置.txt", Gson().toJson(CountData(repeatCount, badRepeatCount)))
                    } else {
                        eventGroupMessage.respond("请设置4到50之间的数")
                    }
                }
            }
            return true
        }
        return false
    }

    private fun 娱乐禁言条数更改命令(eventGroupMessage: EventGroupMessage): Boolean {
        val p = Pattern.compile("#娱乐禁言条数.")
        val m = p.matcher(eventGroupMessage.message)
        if (m.lookingAt()) {
            if (eventGroupMessage.isAdmin(eventGroupMessage.senderId)) {
                val str = eventGroupMessage.message.substringAfter("#娱乐禁言条数").substring(1)
                val mp = Pattern.compile("\\d+")
                val mm = mp.matcher(str)
                if (mm.find()) {
                    val count = mm.group(0).toInt()
                    if (count in 4..50) {
                        repeatCount = count
                        eventGroupMessage.respond("设置成功:$repeatCount")
                        MFile.saveToFile("Data/复读条数设置.txt", Gson().toJson(CountData(repeatCount, badRepeatCount)))
                    } else {
                        eventGroupMessage.respond("请设置4到50之间的数")
                    }
                }
            } else {
                eventGroupMessage.respond("[CQ:at,qq=${eventGroupMessage.senderId}] 您不配")
            }
            return true
        }
        return false
    }

    private fun 敏感关键字处理(eventGroupMessage: EventGroupMessage) {
        forbiddenKeywordList.forEach {
            val p = Pattern.compile(it)
            val m = p.matcher(eventGroupMessage.message)
            if (m.find()) {
                eventGroupMessage.recall()
                eventGroupMessage.ban(60)
            }
        }
    }

    private fun searchEngine(eventGroupMessage: EventGroupMessage): Boolean {
        val p = Pattern.compile("bing搜索[:：]")
        val m = p.matcher(eventGroupMessage.message)
        if (m.lookingAt()) {
            val str = eventGroupMessage.message.substringAfter("bing搜索").substring(1)
            val url = "https://cn.bing.com/search?q=" + URLEncoder.encode(str, "UTF-8")
            eventGroupMessage.respond(url)
            return true
        }

        val p2 = Pattern.compile("csdn搜索[:：]")
        val m2 = p2.matcher(eventGroupMessage.message)
        if (m2.lookingAt()) {
            val str = eventGroupMessage.message.substringAfter("csdn搜索").substring(1)
            val url = "https://so.csdn.net/so/search/s.do?q=" + URLEncoder.encode(str, "UTF-8")
            eventGroupMessage.respond(url)
            return true
        }

        val p3 = Pattern.compile("百度搜索[:：]")
        val m3 = p3.matcher(eventGroupMessage.message)
        if (m3.lookingAt()) {
            val str = eventGroupMessage.message.substringAfter("百度搜索").substring(1)
            val url = "https://m.baidu.com/s?ie=utf-8&f=3&rsv_bp=1&rsv_idx=1&tn=baidu&wd=" + URLEncoder.encode(str, "UTF-8")
            eventGroupMessage.respond(url)
            return true
        }

        val p4 = Pattern.compile("掘金搜索[:：]")
        val m4 = p4.matcher(eventGroupMessage.message)
        if (m4.lookingAt()) {
            val str = eventGroupMessage.message.substringAfter("掘金搜索").substring(1)
            val url = "https://juejin.im/search?query=" + URLEncoder.encode(str, "UTF-8") + "&type=all"
            eventGroupMessage.respond(url)
            return true
        }
        return false
    }

    private fun 封神榜命令(eventGroupMessage: EventGroupMessage): Boolean {
        if (eventGroupMessage.message == "#封神榜") {
            if (entertainmentMessageRankingList.size == 0) {
                eventGroupMessage.respond("还没人复读被禁言呢！慌啥？？")
            } else {
                var str = "封神榜\n" +
                        "***************\n"

                for (i in 0 until entertainmentMessageRankingList.size) {

                    str += "第${i + 1}名：${eventGroupMessage.getGroupUser(entertainmentMessageRankingList[i].qqnum).info.card}\n" +
                            "次数：${entertainmentMessageRankingList[i].count}\n"
                    str += "***************\n"
                    val j = entertainmentMessageRankingList.indexOf(entertainmentMessageRankingList[i])
                    if (i == 4) {
                        break
                    }
                }
                eventGroupMessage.respond(str)
            }
            return true
        }
        return false
    }

    var forbiddenCount = 1
    var forbiddenId = 0.toLong()
    var repeatBanCount = 1
    private fun 娱乐禁言处理(eventGroupMessage: EventGroupMessage) {
        //将当前消息载入
        repeatBanList.add(RepeatData(eventGroupMessage.senderId, eventGroupMessage.message))
        //判断消息是否为2条即以上
        if (repeatBanList.size > 1) {
            if (repeatBanList[0].str == repeatBanList.last.str) {//判断是否为复读消息,不是则清空
                if (repeatBanList.size >= repeatCount) {//判断复读是否超过了设定值
                    val random = Random()
                    val count = random.asKotlinRandom().nextInt(repeatCount)//随机抽取
                    if (forbiddenId == repeatBanList[count].qqnum) {
                        if (eventGroupMessage.getGroupUser(repeatBanList[count].qqnum).isAdmin) {
                            eventGroupMessage.httpApi.sendGroupMsg(group, "" +
                                    "【[CQ:at,qq=${repeatBanList[count].qqnum}]】，咋又是管理员万恶的管理，来来你们继续重复上一句话，我再挑一个")
                        } else {
                            forbiddenCount++//持续禁言计数
                            repeatBanCount++
                            eventGroupMessage.httpApi.sendGroupMsg(group, "" +
                                    "****${forbiddenCount} 杀****\n" +
                                    "恭喜【[CQ:at,qq=${repeatBanList[count].qqnum}]】再次中奖,加一分钟")
                            eventGroupMessage.httpApi.setGroupBan(group, repeatBanList[count].qqnum, 60 * repeatBanCount.toLong())
                            保存至排行榜(count)
                        }

                    } else {
                        if (eventGroupMessage.getGroupUser(repeatBanList[count].qqnum).isAdmin) {
                            eventGroupMessage.httpApi.sendGroupMsg(group, "" +
                                    "【[CQ:at,qq=${repeatBanList[count].qqnum}]】，万恶的管理我禁言不了，来来你们继续重复上一句话，我再挑一个")
                        } else {
                            repeatBanCount = 1
                            forbiddenId = repeatBanList[count].qqnum
                            eventGroupMessage.httpApi.sendGroupMsg(group, "" +
                                    "****${forbiddenCount} 杀****\n" +
                                    "恭喜【[CQ:at,qq=${repeatBanList[count].qqnum}]】获得复读随机禁言套餐")
                            eventGroupMessage.httpApi.setGroupBan(group, repeatBanList[count].qqnum, 60)
                            保存至排行榜(count)
                        }
                    }
                }
            } else {
                repeatBanList.clear()
                forbiddenId = 0
                forbiddenCount = 1
            }
        }
    }

    private fun 保存至排行榜(count: Int) {
        var 是否存在 = false
        entertainmentMessageRankingList.forEach {
            if (repeatBanList[count].qqnum == it.qqnum) {
                it.count += 1
                是否存在 = true
                return@forEach
            }
        }
        if (!是否存在) {
            entertainmentMessageRankingList.add(娱乐禁言Bean(repeatBanList[count].qqnum, 1))
        }
        entertainmentMessageRankingList.sortByDescending { it.count }
        MFile.saveToFile("Data/娱乐禁言排行榜.txt", Gson().toJson(entertainmentMessageRankingList))
    }

    private fun 功能命令处理(eventGroupMessage: EventGroupMessage): Boolean {
        if (eventGroupMessage.message == "功能") {
//            if (eventGroupMessage.isAdmin(eventGroupMessage.senderId)) {
            eventGroupMessage.httpApi.sendPrivateMsg(eventGroupMessage.senderId, "" +
                    "1.百度搜索：XXXX\n" +
                    "2.bing搜索：XXXX\n" +
                    "3.csdn搜索：XXXX\n" +
                    "4.掘金搜索：XXXX(推荐这个)\n" +
                    "5.#恶劣复读禁言条数:XXXX(需要管理员,目前${badRepeatCount}条单人复读当作恶劣复读)\n" +
                    "6.#娱乐禁言条数:XXXX(需要管理员，目前每复读${repeatCount} 条，随机抽一人禁言30秒)\n" +
                    "7.发送（.help）,查看骰子命令\n" +
                    "8.群内有恶劣性敏感词禁言，并且会记录你发送这种铭感词的次数，到达一定数量，会踢出出群聊，请大家文明发言【和谐自由民主】\n" +
                    "9.【娱乐】检测到复读抽取随机禁言大奖，并记录中奖次数\n" +
                    "10.【娱乐】随机禁言排行榜发送（#封神榜）\n" +
                    "【以上所有命令均需要在群里发送】\n" +
                    ""
            )
            eventGroupMessage.respond("[CQ:at,qq=${eventGroupMessage.senderId}] 私发给你了，自己康康")
//            }
            return true
        }
        return false
    }


    //关键字添加者消息处理
    private fun keywordAdderMessageProcessing(eventPrivateMessage: EventPrivateMessage) {
        if (eventPrivateMessage.message == "是") {
            var keywordMessage: KeywordMessage? = null
            keywordsAddMessageQueue.forEach {
                if (eventPrivateMessage.senderId == it.key) {
                    keywordMessage = it.value
                }
            }
            keywordMessage?.let {
                it.lambda.invoke()
            }
            return
        }

        forbiddenKeywordList.forEach {
            if (it == eventPrivateMessage.message) {
                forbiddenKeywordList.remove(it)
                eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "删除成功\n" +
                        "$forbiddenKeywordList")
                MFile.saveToFile("Data/禁言关键词.txt", Gson().toJson(forbiddenKeywordList))
                return
            }
        }


        val p = Pattern.compile("禁言关键字.")
        val m = p.matcher(eventPrivateMessage.message)
        if (m.lookingAt()) {
            val str = eventPrivateMessage.message.substringAfter("禁言关键字").substring(1)
            eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                    "你所添加的关键词是：$str\n" +
                    "是否添加\n" +
                    "请在3分钟之内回复【是】\n")
            val date = Date()
            //禁言消息
            val forbiddenMessage = KeywordMessage(eventPrivateMessage.senderId) {
                val cDate = Date()
                if ((cDate.time - date.time) > 1000 * 60 * 3) {
                    eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "时间超过")
                    keywordsAddMessageQueue.remove(eventPrivateMessage.senderId)
                } else {
                    forbiddenKeywordList.add(str)
                    eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "添加成功\n" +
                            "$forbiddenKeywordList")
                    MFile.saveToFile("Data/禁言关键词.txt", Gson().toJson(forbiddenKeywordList))
                    keywordsAddMessageQueue.remove(eventPrivateMessage.senderId)
                }
            }
            keywordsAddMessageQueue[eventPrivateMessage.senderId] = forbiddenMessage
            return
        }
        eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                "现在有以下关键字：\n" +
                "$forbiddenKeywordList\n" +
                "*********************\n" +
                "直接回复其中的关键字，即可删除\n" +
                "*********************\n" +
                "直接发送下面格式的即可添加关键字\n" +
                "*********************\n" +
                "禁言关键字:XXXXXXXXXX(XXX是内容)\n" +
                "*********************\n" +
                "建议直接复制下面的模板进行修改")
        eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                "禁言关键字：")

    }


    //处理补登请假提示文本
    private fun handlingTheLeaveRequestText(eventPrivateMessage: EventPrivateMessage) {
        when (eventPrivateMessage.senderId) {
            刘旭的QQ, 刘星的QQ, 我的qq -> {
                val dateFormat = SimpleDateFormat()// 格式化时间
                dateFormat.applyPattern("yyyy年MM月dd日")// a为am/pm的标记
                val date = Date()// 获取当前时间
                val time = dateFormat.format(date)
                eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "【提示】第XX节课当中的XX必须在下面之中选择：\n12，34，56，78\n\n\n" +
                        "必须严格按照此模板书写，强烈建议复制以下内容进行更改\n")
                eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "请假人学号：\n" +
                        "请假事由：\n" +
                        "请假班次：${time}第XX节课")
            }
            else -> eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "对不起，你没有权限")
        }
    }

    //处理文档请求消息
    private fun processingDocumentRequestMessage(eventPrivateMessage: EventPrivateMessage) {
        when (eventPrivateMessage.senderId) {
            刘旭的QQ, 刘星的QQ, 我的qq -> {
                createExcel()
                val file = File("C:\\wamp64\\www\\leave\\config\\文档数据标识.txt")
                try {
                    eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "正在导出数据，请稍等...")
                    file.delete()
                    Thread.sleep(3000)
                    eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "http://139.196.143.240/leave/index.php")
                } catch (e: Exception) {
                    Thread.sleep(3000)
                    eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "http://139.196.143.240/leave/index.php")
                }
            }
            else -> eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "对不起，你没有权限")
        }
    }

    //请假流程
    private fun leaveProcess(eventPrivateMessage: EventPrivateMessage) {
        //补登请假
        val p = Pattern.compile("请假人学号：.*[0-9]+.*\r*\n请假事由：.+\r*\n请假班次：20..年..月..日第[1357][2468]节课.*")
        val m = p.matcher(eventPrivateMessage.message)
        if (m.matches()) {
            var student: AssistantStudent? = null
            val mmS = Pattern
                    .compile("[0-9]+")
                    .matcher(eventPrivateMessage.message.substringAfter("请假人学号：")
                            .substringBefore("\n请假事由："))
                    .apply { find() }
            val mStudentID = mmS.group(0)
            val mTime = eventPrivateMessage.message.substringAfter("\n请假班次：")
            val mC = eventPrivateMessage.message.substringAfter("请假事由：").substringBefore("\n请假班次：")
            val mContent = "${mTime}时间段值班请假，事由：${mC}"
            AssistantUtil.assistantDateList.forEach {
                if (it.value.studentID == mStudentID) {
                    student = it.value
                    return@forEach
                }
            }
            if (student == null) {
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "未查到该学生，请检查学号是否输入正确，或者该学生学号未加入配置文档")
            } else {
                val leaveDatasEntity = LeaveDataBean.LeaveDatasEntity().apply {
                    student?.let {
                        name = it.name
                        timestamp = "管理人员补登"
                        content = mContent
                        studentID = it.studentID
                        type = it.type
                        leaveTime = mTime
                    }
                }

                val gson = Gson()
                AssistantUtil.leaveDataBean.leaveDatas.add(leaveDatasEntity)
                FileUtils.writeFile(gson.toJson(AssistantUtil.leaveDataBean))
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "${student?.name}的请假记录补登成功")

                return
            }
        } else {
            val p2 = Pattern.compile("请假人学号.+\r*请假事由.+\r*请假班次.+")
            val m2 = p2.matcher(eventPrivateMessage.message)
            if (m2.find()) {
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "补登正文貌似格式有点问题，按照标准复制改改吧" +
                        "【提示】第XX节课当中的XX务必在：\n12，34，56，78\n")
                return
            }
        }


        //正常请假流程
        val gson = Gson()
        val senderId = eventPrivateMessage.getSenderId()//获得发送消息的人的qq号
//        val senderId = 3517746373//获得发送消息的人的qq号

        if (senderId == 刘旭的QQ) {
            eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "拒绝向帅气的辅导员提供服务。")
            return
        }


        if (senderId == 刘星的QQ) {
            eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                    "功能：（发送功能名称即可）" +
                    "1.补登请假\n" +
                    "2.请假文档（服务器有问题，我有时间再修，你要请假文档可以直接找我（丁）要）")
            return
        }


        var student: AssistantStudent? = null


        for (i in AssistantUtil.assistantDateList) {
            if (i.key.isNotEmpty()) {//防止表格中没有名字的
                if (i.value.qqNumber.replace(Regex("\\s"), "").toLong() == senderId) {
                    student = i.value//若该学生为助理，则获取该学生的详细信息
                    break
                }
            }
        }
        student?.let { assistantStudent ->
            if ("是" == eventPrivateMessage.message) {
                leaveQueue[assistantStudent.name]?.let {
                    synchronized(it) {
                        leaveQueue[assistantStudent.name]?.lambda?.invoke()?.let { leaveDatasEntity ->
                            var date: Calendar? = null
                            Calendar.getInstance().apply {
                                time = strToDate(leaveDatasEntity?.timestamp ?: "")
                                date = this
                            }
                            val today: Calendar? = null
                            Calendar.getInstance().apply {
                                time = Date()
                                date = this
                            }

                            if (today?.get(Calendar.DAY_OF_MONTH) == date?.get(Calendar.DAY_OF_MONTH)) {
                                if (assistantStudent.type == "实验室助理") {
                                    val stringBuilder = StringBuilder()
                                    eventPrivateMessage.httpApi.sendPrivateMsg(我的qq, "" +
                                            "${leaveDatasEntity.type}-${leaveDatasEntity.name}请假：\n\n" +
                                            leaveDatasEntity.content)
                                    stringBuilder.append("【${assistantStudent.name}】")
                                    stringBuilder.append("临时请假，请各位老师注意\n")
                                    eventPrivateMessage.httpApi.sendGroupMsg(436641186, stringBuilder.toString())
                                    eventPrivateMessage.httpApi.sendGroupMsg(451094615, stringBuilder.toString())
                                } else {
                                    eventPrivateMessage.httpApi.sendPrivateMsg(刘星的QQ, "" +
                                            "${leaveDatasEntity.type}-${leaveDatasEntity.name}请假：\n\n" +
                                            leaveDatasEntity.content)
                                    val stringBuilder = StringBuilder()
                                    stringBuilder.append("【${assistantStudent.name}】")
                                    stringBuilder.append("临时请假，请各位老师注意")
                                    eventPrivateMessage.httpApi.sendGroupMsg(286199556, stringBuilder.toString())
                                }
                            }
                            if (assistantStudent.type == "实验室助理") {
                                eventPrivateMessage.httpApi.sendPrivateMsg(我的qq, "" +
                                        "${leaveDatasEntity.type}-${leaveDatasEntity.name}请假：\n\n" +
                                        leaveDatasEntity.content)
                            } else {
                                eventPrivateMessage.httpApi.sendPrivateMsg(刘星的QQ, "" +
                                        "${leaveDatasEntity.type}-${leaveDatasEntity.name}请假：\n\n" +
                                        leaveDatasEntity.content)
                            }
                            leaveQueue.remove(assistantStudent.name)
                            return@leaveProcess
                        }

                    }
                }
            }


            val pattern = Pattern.compile("20..年..月..日第[1357][2468]节课时间段值班请假.事由..+")
            val matcher = pattern.matcher(eventPrivateMessage.message)
            if (matcher.find()) {
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "是否确认请假？【是】\n" +
                        "确认请在3分钟内回复【是】\n" +
                        "若不确认该请假会在3分钟后自动销毁！")
                val leaveDatasEntity = LeaveDataBean.LeaveDatasEntity().apply {
                    val dateFormat = SimpleDateFormat()// 格式化时间
                    dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss")// a为am/pm的标记
                    val date = Date()// 获取当前时间
                    assistantStudent.let {
                        name = it.name
                        timestamp = dateFormat.format(date)
                        content = eventPrivateMessage.message
                        studentID = it.studentID
                        type = it.type
                        leaveTime = eventPrivateMessage.message.substringBefore("时间段")

                    }
                }

                val leaveQueueMessage = LeaveQueueMessage(assistantStudent.name) {
                    AssistantUtil.leaveDataBean.leaveDatas.add(leaveDatasEntity)
                    FileUtils.writeFile(gson.toJson(AssistantUtil.leaveDataBean))
                    eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "请假成功")
                    leaveDatasEntity
                }

                leaveQueue.remove(assistantStudent.name)
                leaveQueue[assistantStudent.name] = leaveQueueMessage
                ThreadPoolUtils.execute {
                    Thread.sleep(1000 * 60 * 3)
                    leaveQueue.remove(assistantStudent.name)
                }
            } else {
                val pattern = Pattern.compile(".*年.*月.*日第.*节课时间段值班请假.*事由")
                val matcher = pattern.matcher(eventPrivateMessage.message)
                if (matcher.find()) {
                    eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                            "刚刚的请假正文貌似格式有点问题，按照标准复制改改吧！\n" +
                            "可能没有请假事由，或者日期有误")
                    return
                }
                val massage = (
                        "${assistantStudent.type}-${assistantStudent.name}，你好！！\n\n" +
                                "若请假请直接发送请假事由，格式如下：\n******************\n\n" +
                                "XXXX年XX月XX日第XX节课时间段值班请假，事由：XXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\n" +
                                "******************\n" +
                                "请假成功会有文字回复。\n" +
                                "【提示】请勿多次尝试性发送请假消息,不然最后统计请假次数过多就翻车了。编辑好了直接发给机器人\n" +
                                "【提示】第XX节课当中的XX务必在：\n12，34，56，78\n" +
                                "这四个选项中选择" +
                                "\n******************\n" +
                                "可直接复制以下模板更改\n").trimMargin()
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, massage)
                val dateFormat = SimpleDateFormat()// 格式化时间
                dateFormat.applyPattern("yyyy年MM月dd日")// a为am/pm的标记
                val date = Date()// 获取当前时间
                val time = dateFormat.format(date)
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "${time}第XX节课时间段值班请假，事由：")
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "请给出合理事由，请假成功之后消息会被实时发送到管理者的qq里，若事由太过随意，会根据情况降低你的最终评价，从而影响时长或者每月补贴")
            }
        }

    }

    /**
         * 判断时间是否在时间段内
         * 
         * @param nowTime
         * @param beginTime
         * @param endTime
         * @return
         */
    public fun belongCalendar(nowTime: Date, beginTime: Date, endTime: Date): Boolean {
        val date = Calendar.getInstance();
        date.time = nowTime;
        val begin = Calendar.getInstance();
        begin.time = beginTime;
        val end = Calendar.getInstance();
        end.time = endTime;
        return if (date.after(begin) && date.before(end)) {
            true;
        } else nowTime.compareTo(beginTime) == 0 || nowTime.compareTo(endTime) == 0
    }

    // 字符串 转 日期
    fun strToDate(str: String): Date? {
        if (str.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            val date = sdf.parse(str);
            return date;
        } else {
            return null

        }
    }

}