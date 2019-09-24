package tool

import bean.AssistantStudent
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.CellType
import java.io.*
import java.util.*
import java.util.regex.Pattern
import javax.swing.filechooser.FileSystemView
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.hssf.record.cf.BorderFormatting.BORDER_THIN
import org.apache.poi.ss.usermodel.HorizontalAlignment


object FileUtils {


    fun writeFile(content:String) {
//        val currentDir = System.getProperty("user.dir") + "\\out"
        val currentDir = """C:\data\请假数据"""
        val file = File(currentDir, "leaveData.txt")
        file.writeText(content)
//        println(file.readText())

//
//        //直接使用writer和outputstream
//        val writer: Writer = file.writer()
//        val outputStream: OutputStream = file.outputStream()
//        val printWriter: PrintWriter = file.printWriter()

    }


    fun writeFileAdd() {
        val currentDir = System.getProperty("user.dir") + "\\out"
        val file = File(currentDir, "leaveData.txt")
        //追加方式写入字节或字符
        file.appendBytes(byteArrayOf(93, 85, 74, 93))
        file.appendText("吼啊")
        println(file.readText())
    }


    fun readStudentDataExcel() {
//        val fsv = FileSystemView.getFileSystemView()
//        val desktop = fsv.homeDirectory.path
        val filePath = """C:\data\请假数据\StudentsDate.xls"""

        val fileInputStream = FileInputStream(filePath)
        val bufferedInputStream = BufferedInputStream(fileInputStream)
        val fileSystem = POIFSFileSystem(bufferedInputStream)
        val workbook = HSSFWorkbook(fileSystem)
        val sheet = workbook.getSheetAt(0)

        val lastRowIndex = sheet.lastRowNum
        for (i in 1..lastRowIndex) {
            val row = sheet.getRow(i) ?: break
            val lastCellNum = row.lastCellNum
            var 是否记录本行 = false
            for (j in 0 until lastCellNum) {
                if (row.getCell(j) != null) {
                    是否记录本行 = true
                }
                row.getCell(j)?.cellType = CellType.STRING
            }
            if (是否记录本行) {
                row.getCell(2)?.let {
                    AssistantUtil.assistantDateList[it.stringCellValue] =
                    AssistantStudent().apply {
                        for (j in 0 until lastCellNum) {
                            row.getCell(j)?.let {
                                val p = Pattern.compile("[\\u2E80-\\u9FFF0-9a-zA-Z]+")
                                val m = p.matcher(it.stringCellValue)
                                if (m.find()) {
                                    this[j] = m.group(0)
                                }
                            }
                        }
                        print("")
                    }
                }
            }
        }
        bufferedInputStream.close()
    }


    fun createExcel() {
        // 获取桌面路径
        val filePath = "C:\\wamp64\\www\\leave\\请假记录文档.xls"

        val file = File(filePath)
        val outputStream = FileOutputStream(file)
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")
        val row = sheet.createRow(0)
        row.createCell(0).setCellValue("学工类型")
        row.createCell(1).setCellValue("学号")
        row.createCell(2).setCellValue("姓名")
        row.createCell(3).setCellValue("事由")
        row.createCell(4).setCellValue("请假班次")
        row.createCell(5).setCellValue("何时请的假")


        for (i in AssistantUtil.leaveDataBean.leaveDatas) {
            val row1 = sheet.createRow(AssistantUtil.leaveDataBean.leaveDatas.indexOf(i)+1)
            row1.createCell(0).setCellValue(i.type)
            row1.createCell(1).setCellValue(i.studentID)
            row1.createCell(2).setCellValue(i.name)
            row1.createCell(3).setCellValue(i.content.substringAfter("节课时间段值班请假").substring(4))
            row1.createCell(4).setCellValue(i.leaveTime)
            row1.createCell(5).setCellValue(i.timestamp)

        }

        // 单元格样式
        val cellStyle = workbook.createCellStyle()
        cellStyle.alignment = HorizontalAlignment.CENTER
        //设置自动换行
        cellStyle.wrapText = true

        for (i in 0 until 6) {
            sheet.autoSizeColumn(i);//先设置自动列宽
            sheet.setColumnWidth(i,sheet.getColumnWidth(i)*17/10);//设置列宽为自动列宽的1.7倍（当然不是严格的1.7倍，int的除法恕不再讨论），这个1.6左右也可以，这是本人测试的经验值*
        }

        workbook.setActiveSheet(0)
        workbook.write(outputStream)
        outputStream.close()
    }




    fun readFile():String{
        val filename = """C:\data\请假数据\leaveData.txt"""
        val file = File(filename)
        return file.readText()
//        println(contents)
//
//        //大写前三行
//        file.readLines().take(3).forEach {
//            println(it.toUpperCase())
//        }
//
//        //直接处理行
//        file.forEachLine(action = ::println)
//
//        //读取为bytes
//        val bytes: ByteArray = file.readBytes()
//        println(bytes.joinToString(separator = ""))
//
//        //直接处理Reader或InputStream
//        val reader: Reader = file.reader()
//        val inputStream: InputStream = file.inputStream()
//        val bufferedReader: BufferedReader = file.bufferedReader()
    }

}

