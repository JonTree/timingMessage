package tool
import tool.FileUtils.readOnDutyDataExcel
import tool.FileUtils.readStudentDataExcel
import java.text.SimpleDateFormat
import java.util.*


object AssistantUtil {
    init {
        //导入学生数据
        readStudentDataExcel()
        //导入学生班次数据
        readOnDutyDataExcel()
    }


    fun dayForWeek(pTime: String): Int {
        val format = SimpleDateFormat("yyyy-MM-dd")
        val c = Calendar.getInstance()
        c.time = format.parse(pTime)
        return if (c.get(Calendar.DAY_OF_WEEK) == 1) {
            7
        } else {
            c.get(Calendar.DAY_OF_WEEK) - 1
        }
    }


    fun dayForTime(pTime: String):Int {
        return when (pTime.substringBeforeLast(":")) {
            "07:45" -> 1
            "10:00" -> 2
            "13:45" -> 3
            "16:00" -> 4
            else -> 0
        }
    }

    fun period(position: Int):String? {
        return when (position) {
            1 -> "8:00-9:40"
            2 -> "10:15-11:55"
            3 -> "14:00-15:40"
            4 -> "16:15-17:55"
            else -> null
        }
    }
}