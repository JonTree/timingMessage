package bean

data class AssistantStudent(var name: String = "", var studentID:String = "",var type:String = "", var phoneNumber: String = "", var qqNumber: String = ""){
    val list = mutableListOf(type,studentID,name,qqNumber,phoneNumber)
    operator fun get(index: Int) = list[index]

    operator fun set(index: Int,value: String) {
        list[index] = value
        when (index) {
            0 -> type = value
            1 -> studentID = value
            2 -> name = value
            3 -> qqNumber = value
            4 -> phoneNumber = value

        }
    }
}
