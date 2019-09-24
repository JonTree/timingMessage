package tool
import bean.AssistantStudent
import bean.LeaveDataBean
import com.google.gson.Gson
import java.util.Calendar
import java.text.SimpleDateFormat
import tool.FileUtils.readStudentDataExcel


object AssistantUtil {
    val laboratoryWatchList = HashMap<Int, List<String>>()//实验室
    val collegeWatchList = HashMap<Int, List<String>>()//院办
    val schoolRollWatchList = HashMap<Int, List<String>>()//学籍办
    val academicWorkerWatchList = HashMap<Int, List<String>>()//学工办


    val assistantDateList = HashMap<String, AssistantStudent>()
    var leaveDataBean: LeaveDataBean = LeaveDataBean()


    init {
        val gson = Gson()
        val date = FileUtils.readFile()
       if (date.isNotEmpty()) {
           leaveDataBean =  gson.fromJson<LeaveDataBean>(date, LeaveDataBean::class.java)?: LeaveDataBean()
        }


        readStudentDataExcel()

        //实验室助理
        laboratoryWatchList[11] = listOf("敬清清", "张文江")
        laboratoryWatchList[13] = listOf("周香伶","唐鑫")
        laboratoryWatchList[14] = listOf("王君然","夏燚")
        laboratoryWatchList[21] = listOf("张文江", "刘大泽")
        laboratoryWatchList[22] = listOf("丁德桥", "李月月")
        laboratoryWatchList[23] = listOf("朱榆涛")
        laboratoryWatchList[24] = listOf("李鑫")
        laboratoryWatchList[32] = listOf("苏亚州")
        laboratoryWatchList[33] = listOf("王澳歌","刘浩瑜")
        laboratoryWatchList[34] = listOf("丁德桥","朱榆涛")
        laboratoryWatchList[41] = listOf("张川","刘大泽")
        laboratoryWatchList[42] = listOf("周香伶","唐鑫")
        laboratoryWatchList[43] = listOf("王澳歌","夏燚")
        laboratoryWatchList[44] = listOf("李鑫", "王君然")
        laboratoryWatchList[52] = listOf("苏亚州")
        laboratoryWatchList[53] = listOf("张川", "李月月")
        laboratoryWatchList[54] = listOf("刘浩瑜","敬清清")


        //院办助理
        collegeWatchList[11] = listOf("周庚辰","钱瀛天")
        collegeWatchList[12] = listOf("番能赞","邱川江")
        collegeWatchList[13] = listOf("金泽旺")
        collegeWatchList[14] = listOf("凤余","张宇")
        collegeWatchList[21] = listOf("刘伟","钱瀛天")
        collegeWatchList[22] = listOf("范记平")
        collegeWatchList[23] = listOf("兰明坤","鞠祯")
        collegeWatchList[24] = listOf("董晨")
        collegeWatchList[31] = listOf("金泽旺","李浩林")
        collegeWatchList[32] = listOf("兰明坤","鞠祯")
        collegeWatchList[33] = listOf("范记平","国旭")
        collegeWatchList[34] = listOf("李权树")
        collegeWatchList[41] = listOf("番能赞")
        collegeWatchList[42] = listOf("董晨","刘伟")
        collegeWatchList[43] = listOf("肖锋","宋尹乐")
        collegeWatchList[44] = listOf("周庚辰","石浪")
        collegeWatchList[51] = listOf("李浩林","李权树")
        collegeWatchList[52] = listOf("邱川江")
        collegeWatchList[53] = listOf("肖锋","石浪")
        collegeWatchList[54] = listOf("国旭","饶钙")
        //学籍办助理


        schoolRollWatchList[11] = listOf("王会娜","蔡溶霄")
        schoolRollWatchList[12] = listOf("桂书婷")
        schoolRollWatchList[13] = listOf("廖彦婷","张祺")
        schoolRollWatchList[14] = listOf("刘顺舟","张曜昕")
        schoolRollWatchList[21] = listOf("王会娜","贺晨星")
        schoolRollWatchList[22] = listOf("唐浩彬")
        schoolRollWatchList[23] = listOf("龚爽")
        schoolRollWatchList[24] = listOf("叶兆玲")
        schoolRollWatchList[31] = listOf("吴昊")
        schoolRollWatchList[32] = listOf("龚爽","桂书婷")
        schoolRollWatchList[33] = listOf("廖彦婷","张祺")
        schoolRollWatchList[34] = listOf("王心怡")
        schoolRollWatchList[41] = listOf("吴昊","刘鑫瑞")
        schoolRollWatchList[42] = listOf("叶兆玲")
        schoolRollWatchList[43] = listOf("陈婧")
        schoolRollWatchList[44] = listOf("刘顺舟","贺晨星")
        schoolRollWatchList[51] = listOf("王心怡","舒玉伟")
        schoolRollWatchList[52] = listOf("唐浩彬","罗浩")
        schoolRollWatchList[53] = listOf("陈婧")
        schoolRollWatchList[54] = listOf("蔡溶霄")

        //学工办
        academicWorkerWatchList[11] = listOf("李文豪","许露月")
        academicWorkerWatchList[12] = listOf("李佳仪","刘初阳")
        academicWorkerWatchList[13] = listOf("任科宇")
        academicWorkerWatchList[14] = listOf("金家昊","杨代辉")
        academicWorkerWatchList[21] = listOf("李文豪")
        academicWorkerWatchList[22] = listOf("周正")
        academicWorkerWatchList[23] = listOf("刘杰")
        academicWorkerWatchList[24] = listOf("张江")
        academicWorkerWatchList[31] = listOf("刘杰","张江")
        academicWorkerWatchList[32] = listOf("文流彬")
        academicWorkerWatchList[33] = listOf("胡兴欣","李佳仪")
        academicWorkerWatchList[34] = listOf("宋万伟")
        academicWorkerWatchList[41] = listOf("刘初阳")
        academicWorkerWatchList[43] = listOf("郑福恒","杨代辉")
        academicWorkerWatchList[44] = listOf("胡兴欣","王艺彦")
        academicWorkerWatchList[51] = listOf("许露月")
        academicWorkerWatchList[52] = listOf("文流彬")
        academicWorkerWatchList[53] = listOf("周正","王艺彦")
        academicWorkerWatchList[54] = listOf("金家昊","郑福恒")

    }

    fun dayForWeek(pTime: String): Int {
        val format = SimpleDateFormat("yyyy-MM-dd")
        val c = Calendar.getInstance()
        c.time = format.parse(pTime)
        var dayForWeek = 0
        dayForWeek = if (c.get(Calendar.DAY_OF_WEEK) == 1) {
            7
        } else {
            c.get(Calendar.DAY_OF_WEEK) - 1
        }
        return dayForWeek
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