package tool

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor


object ThreadPoolUtils {

    private val threadPoolExecutor: ThreadPoolExecutor = Executors.newFixedThreadPool(5) as ThreadPoolExecutor//线程池的


    fun execute(task: ()->Unit) {
        threadPoolExecutor.execute { task }
    }

    //一个有返回值的执行方法
    fun <T> submit(task: Callable<T>): Future<T> {
        val futureTask = FutureTask(task)
        threadPoolExecutor.submit(futureTask)
        return futureTask
    }


    //一个可以更新UI的执行方法
}
