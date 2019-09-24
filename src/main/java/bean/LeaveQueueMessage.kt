package bean

import java.lang.invoke.SerializedLambda

data class LeaveQueueMessage(val name: String,val lambda: ()->LeaveDataBean.LeaveDatasEntity)