package listen

import bean.AssistantStudent
import tool.AssistantUtil
import tool.FileUtils
import bean.LeaveDataBean
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
import bean.LeaveQueueMessage
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import java.lang.invoke.SerializedLambda
import java.net.SocketTimeoutException


class AskForLeaveEventPrivateMessage : IcqListener() {

    private val leaveQueue = HashMap<String, LeaveQueueMessage>()

    val 刘星的QQ = 1623044256.toLong()
    val 刘旭的QQ = 2108372936.toLong()
    val 我的qq = 1246634075.toLong()

    @EventHandler
    fun saveTheLeaveEvent(eventPrivateMessage: EventPrivateMessage) {
        when (eventPrivateMessage.message) {
            "请假文档" -> 处理文档请求消息(eventPrivateMessage)
            "补登请假" -> 处理补登请假提示文本(eventPrivateMessage)
            else -> 请假流程(eventPrivateMessage)
        }

    }


    fun 处理补登请假提示文本(eventPrivateMessage: EventPrivateMessage) {
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

    fun 处理文档请求消息(eventPrivateMessage: EventPrivateMessage) {
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

    fun 请假流程(eventPrivateMessage: EventPrivateMessage) {
        //补登请假
        val p = Pattern.compile("请假人学号：.*[0-9]+.*\r*\n请假事由：.+\r*\n请假班次：20..年..月..日第[1357][2468]节课.*")
        val m = p.matcher(eventPrivateMessage.message)
        if (m.matches()) {
            val str = eventPrivateMessage.message
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
            val m2 = p.matcher(eventPrivateMessage.message)
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
//        val senderId = 2470137127//获得发送消息的人的qq号

        if (senderId == 刘旭的QQ) {
            eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "拒绝向帅气的辅导员提供服务。")
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
                        val leaveDatasEntity = leaveQueue[assistantStudent.name]?.lambda?.invoke()

                        val date = strToDate(leaveDatasEntity?.timestamp?:"")
                        if (assistantStudent.type == "实验室助理") {
                            val stringBuilder = StringBuilder()
                            stringBuilder.append("【${assistantStudent.name}】")
                            stringBuilder.append("有事请假，请各位老师注意")
                            eventPrivateMessage.httpApi.sendGroupMsg(436641186, stringBuilder.toString())
                            eventPrivateMessage.httpApi.sendGroupMsg(451094615, stringBuilder.toString())

                        } else {
                            val stringBuilder = StringBuilder()
                            stringBuilder.append("【${assistantStudent.name}】")
                            stringBuilder.append("有事请假，请各位老师注意")
                            eventPrivateMessage.httpApi.sendGroupMsg(286199556, stringBuilder.toString())
                        }
                        leaveQueue.remove(assistantStudent.name)
                        return@请假流程
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
                    Runtime.getRuntime().exec("sh /LeaveDate/push.sh > out.txt")
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
                                "可直接复制以下模板更改").trimMargin()
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, massage)
                val dateFormat = SimpleDateFormat()// 格式化时间
                dateFormat.applyPattern("yyyy年MM月dd日")// a为am/pm的标记
                val date = Date()// 获取当前时间
                val time = dateFormat.format(date)
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "${time}第XX节课时间段值班请假，事由：")
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
        }else{
            return null

        }
    }

}