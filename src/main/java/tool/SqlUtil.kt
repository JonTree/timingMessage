package tool

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class SqlUtil{
    private val DATABASE = "leaveBot"
    private val DB_URL = "jdbc:mysql://localhost:3306/$DATABASE?useSSL=false&serverTimezone=UTC";
    private val JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private val USER = "root"
    private val PASS = "[Tree]whoami8915"
    private var conn: Connection
    var statement: Statement
    init {
        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS)
        statement = conn.createStatement()
    }

    fun close() {
        statement.close()
        conn.close()
    }
}