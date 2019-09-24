package bean;

import java.util.ArrayList;
import java.util.List;

public class LeaveDataBean {

    /**
     * leaveDatas : [{"studentID":"9090909","Content":"hhhhhhhh","name":"张三","timestamp":888888}]
     */
    private List<LeaveDatasEntity> leaveDatas = new ArrayList<LeaveDatasEntity>();

    public void setLeaveDatas(List<LeaveDatasEntity> leaveDatas) {
        this.leaveDatas = leaveDatas;
    }

    public List<LeaveDatasEntity> getLeaveDatas() {
        return leaveDatas;
    }

    public static class LeaveDatasEntity {
        /**
         * studentID : 9090909
         * Content : hhhhhhhh
         * name : 张三
         * timestamp : 888888
         */
        private String studentID;
        private String Content;
        private String name;
        private String timestamp;
        private String type;
        private String leaveTime;

        public String getLeaveTime() {
            return leaveTime;
        }

        public void setLeaveTime(String leaveTime) {
            this.leaveTime = leaveTime;
        }



        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setStudentID(String studentID) {
            this.studentID = studentID;
        }

        public void setContent(String Content) {
            this.Content = Content;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStudentID() {
            return studentID;
        }

        public String getContent() {
            return Content;
        }

        public String getName() {
            return name;
        }

    }
}
