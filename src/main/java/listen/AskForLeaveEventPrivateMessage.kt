package listen

import bean.AssistantStudent
import bean.LeaveDataBean
import bean.LeaveQueueMessage
import cc.moecraft.icq.event.EventHandler
import cc.moecraft.icq.event.IcqListener
import cc.moecraft.icq.event.events.message.EventPrivateMessage
import tool.FileUtils.createExcel
import tool.SqlUtil
import tool.ThreadPoolUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

class AskForLeaveEventPrivateMessage : IcqListener() {

    private val leaveQueue = HashMap<String, LeaveQueueMessage>()

    val 刘星的QQ = 1623044256.toLong()
    val 刘旭的QQ = 2108372936.toLong()
    val 程碧勇的qq = 1335088824.toLong()
    val 杨天的qq = 1049895891.toLong()
    val 我的qq = 1246634075.toLong()


    @EventHandler
    fun saveTheLeaveEvent(eventPrivateMessage: EventPrivateMessage) {
        assistantGroupRobot(eventPrivateMessage)
    }

    /**
     * 助理群机器人
     */
    private fun assistantGroupRobot(eventPrivateMessage: EventPrivateMessage) {
        when (eventPrivateMessage.message) {
            "请假文档" -> processingDocumentRequestMessage(eventPrivateMessage)
            "补登请假" -> handlingTheLeaveRequestText(eventPrivateMessage)
            else -> leaveProcess(eventPrivateMessage)
        }
    }

    //处理补登请假提示文本
    private fun handlingTheLeaveRequestText(eventPrivateMessage: EventPrivateMessage) {
        when (eventPrivateMessage.senderId) {
            刘旭的QQ, 刘星的QQ, 我的qq, 程碧勇的qq, 杨天的qq -> {
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

    /**
     * 处理请求文档的请求
     */
    private fun processingDocumentRequestMessage(eventPrivateMessage: EventPrivateMessage) {
        when (eventPrivateMessage.senderId) {
            刘旭的QQ, 刘星的QQ, 我的qq, 程碧勇的qq, 杨天的qq -> {
                createExcel()
                val file = File("C:\\wamp64\\www\\leave\\config\\文档数据标识.txt")
                try {
                    eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "正在导出数据，请稍等...")
                    file.delete()
                    delaySendingDownloadLinks(eventPrivateMessage)
                } catch (e: Exception) {
                    delaySendingDownloadLinks(eventPrivateMessage)
                }
            }
            else -> eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "对不起，你没有权限")
        }
    }

    /**
     * 延迟发送下载链接
     */
    private fun delaySendingDownloadLinks(eventPrivateMessage: EventPrivateMessage) {
        Thread.sleep(2000)
        eventPrivateMessage.httpApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                "http://139.196.143.240/leave/index.php")
    }

    /**
     * 请假流程
     */
    private fun leaveProcess(eventPrivateMessage: EventPrivateMessage) {
        val sqlUtil = SqlUtil()
        //补登请假
        val p = Pattern.compile("请假人学号：.*[0-9]+.*\r*\n请假事由：.+\r*\n请假班次：20..年..月..日第[1357][2468]节课.*")
        val m = p.matcher(eventPrivateMessage.message)
        if (m.matches()) {
            val mmS = Pattern
                    .compile("[0-9]+")
                    .matcher(eventPrivateMessage.message.substringAfter("请假人学号：")
                            .substringBefore("\n请假事由："))
                    .apply { find() }
            val mStudentID = mmS.group(0)
            val mTime = eventPrivateMessage.message.substringAfter("\n请假班次：")
            val mC = eventPrivateMessage.message.substringAfter("请假事由：").substringBefore("\n请假班次：")
            val mContent = "${mTime}时间段值班请假，事由：${mC}"
            //Sql
            val resultSet = sqlUtil.statement.executeQuery("select * from assistantstudent  where studentId=${mStudentID};")
            if (resultSet.first()) {
                val leaveDatasEntity = LeaveDataBean.LeaveDatasEntity().apply {
                    name = resultSet.getString("name")
                    timestamp = "管理人员补登"
                    content = mContent
                    studentID = resultSet.getLong("studentId").toString()
                    type = resultSet.getString("type")
                    leaveTime = mTime
                }
                insertLeaveData(leaveDatasEntity)
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "${leaveDatasEntity.name}的请假记录补登成功")
                return
            } else {
                eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                        "未查到该学生，请检查学号是否输入正确，或者该学生学号未加入配置文档")
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
        val senderId = eventPrivateMessage.getSenderId()//获得发送消息的人的qq号

        if (senderId == 刘旭的QQ) {
            eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "拒绝向帅气的辅导员提供服务。")
            return
        }


        if (senderId == 刘星的QQ || senderId == 程碧勇的qq || senderId == 杨天的qq) {
            eventPrivateMessage.bot.accountManager.nonAccountSpecifiedApi.sendPrivateMsg(eventPrivateMessage.senderId, "" +
                    "功能：（发送功能名称即可）\n" +
                    "1.补登请假\n" +
                    "2.请假文档")
            return
        }


        var student: AssistantStudent? = null

        sqlUtil.statement.executeQuery("select * from assistantstudent where qqNumber = '${senderId}'").apply {
            if (first()) {
                val name = getString("name")
                val studentId = getString("studentId")
                val type = getString("type")
                val phoneNumber = getString("phoneNumber")
                val qqNumber = getString("qqNumber")
                student = AssistantStudent(name, studentId, type, phoneNumber, qqNumber)
            }
        }


        student?.let { assistantStudent ->
            if ("是" == eventPrivateMessage.message) {
                leaveQueue[assistantStudent.name]?.let {
                    synchronized(it) {
                        leaveQueue[assistantStudent.name]?.lambda?.invoke()?.let { leaveDatasEntity ->
                            var date: Calendar?
                            Calendar.getInstance().apply {
                                time = strToDate(leaveDatasEntity.timestamp ?: "")
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
                                    sendNotify(eventPrivateMessage, leaveDatasEntity,我的qq)
                                    stringBuilder.append("【${assistantStudent.name}】")
                                    stringBuilder.append("临时请假，请各位老师注意\n")
                                    eventPrivateMessage.httpApi.sendGroupMsg(436641186, stringBuilder.toString())
                                    eventPrivateMessage.httpApi.sendGroupMsg(451094615, stringBuilder.toString())
                                } else {
                                    sendNotify(eventPrivateMessage, leaveDatasEntity,刘星的QQ, 程碧勇的qq,杨天的qq)
                                    val stringBuilder = StringBuilder()
                                    stringBuilder.append("【${assistantStudent.name}】")
                                    stringBuilder.append("临时请假，请各位老师注意")
                                    eventPrivateMessage.httpApi.sendGroupMsg(286199556, stringBuilder.toString())
                                }
                            }
                            if (assistantStudent.type == "实验室助理") {
                                sendNotify(eventPrivateMessage,leaveDatasEntity,我的qq)
                            } else {
                                sendNotify(eventPrivateMessage, leaveDatasEntity,刘星的QQ, 程碧勇的qq,杨天的qq)
                            }
                            leaveQueue.remove(assistantStudent.name)
                            createExcel()
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
                    insertLeaveData(leaveDatasEntity)
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
                val pattern1 = Pattern.compile(".*年.*月.*日第.*节课时间段值班请假.*事由")
                val matcher1 = pattern1.matcher(eventPrivateMessage.message)
                if (matcher1.find()) {
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
        sqlUtil.close()
    }

    private fun sendNotify(eventPrivateMessage: EventPrivateMessage, leaveDatasEntity: LeaveDataBean.LeaveDatasEntity, vararg qqNums: Long) {
        qqNums.forEach {
            eventPrivateMessage.httpApi.sendPrivateMsg(it, "" +
                    "${leaveDatasEntity.type}-${leaveDatasEntity.name}请假：\n\n" +
                    leaveDatasEntity.content)
        }
    }

    private fun insertLeaveData(leaveDatasEntity: LeaveDataBean.LeaveDatasEntity) {
        val sqlUtil = SqlUtil()
        val sql = leaveDatasEntity.let {
            "insert into leavebot.leavedata (studentId, type, name, content, leaveTime,time) values (${it.studentID},'${it.type}','${it.name}','${it.content}','${it.leaveTime}','${it.timestamp}');"
        }
        sqlUtil.statement.execute(sql)
        sqlUtil.close()
    }

    // 字符串 转 日期
    private fun strToDate(str: String): Date? =
            if (str.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                val date = sdf.parse(str);
                date;
            } else {
                null
            }
}