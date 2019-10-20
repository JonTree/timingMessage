package bean

import java.lang.invoke.SerializedLambda
import java.util.*

data class LeaveQueueMessage(val name: String,val lambda: ()->LeaveDataBean.LeaveDatasEntity)

/**
 * 关键字消息
 */
data class KeywordMessage(val qqNum: Long, val lambda: () -> Unit)
