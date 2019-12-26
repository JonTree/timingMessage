package tool

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

object SqlUtil{
    private const val DATABASE = "leaveBot"
    private const val DB_URL = "jdbc:mysql://localhost:3306/$DATABASE?useSSL=false&serverTimezone=UTC";
    private const val JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private const val USER = "root"
    private const val PASS = "[Tree]whoami8915"
    private var conn: Connection = Any().let {
        Class.forName(JDBC_DRIVER);
        DriverManager.getConnection(DB_URL, USER, PASS)
    }
    var statement: Statement = conn.createStatement()
}