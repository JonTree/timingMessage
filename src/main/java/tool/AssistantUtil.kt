package tool
import bean.AssistantStudent
import bean.LeaveDataBean
import com.google.gson.Gson
import tool.FileUtils.readOnDutyDataExcel
import java.text.SimpleDateFormat
import tool.FileUtils.readStudentDataExcel
import java.util.*
import kotlin.collections.HashMap


object AssistantUtil {
    val laboratoryWatchList = HashMap<Int, MutableList<String>>()//实验室
    val collegeWatchList = HashMap<Int, MutableList<String>>()//院办
    val schoolRollWatchList = HashMap<Int, MutableList<String>>()//学籍办
    val academicWorkerWatchList = HashMap<Int, MutableList<String>>()//学工办
    val assistantDateList = HashMap<String, AssistantStudent>()
    var leaveDataBean: LeaveDataBean = LeaveDataBean()


    init {
        val gson = Gson()
        val date = FileUtils.readFile()
       if (date.isNotEmpty()) {
           leaveDataBean =  gson.fromJson<LeaveDataBean>(date, LeaveDataBean::class.java)?: LeaveDataBean()
        }

        for (i in 1..5) {
            for (j in 1..4) {
                laboratoryWatchList["$i$j".toInt()] = mutableListOf()
                collegeWatchList["$i$j".toInt()] = mutableListOf()
                schoolRollWatchList["$i$j".toInt()] = mutableListOf()
                academicWorkerWatchList["$i$j".toInt()] = mutableListOf()
            }
        }
        //导入学生数据
        readStudentDataExcel()
        //导入学生班次数据
        readOnDutyDataExcel()

    }

    fun lookingForAPlaceToWork(place:String)=
            when(place){
                "实验室助理" -> laboratoryWatchList
                "院办助理" -> collegeWatchList
                "学籍办助理" -> schoolRollWatchList
                "学工办助理" -> academicWorkerWatchList
                else -> null
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

    fun determineIfItIsWithinTheTimePeriod(date:Date,startDate: Date,endDate: Date)= startDate.time < date.time&&endDate.time>date.time
}