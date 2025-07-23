package cp.serverPr

import scala.collection.mutable.{ListBuffer, Queue}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

class ServerState {
  private val MAX_RUNNING = 3 // CHANGE AS NEEDED !!!
  private val counter = new AtomicInteger(0) 
  private val runningProcesses = new AtomicReference[ListBuffer[(String, String, Int)]](ListBuffer.empty) 
  private val pendingQueue = new AtomicReference[Queue[(String, String)]](Queue.empty) 

  def incrementAndGetCounter: Int = counter.incrementAndGet()
  
  def canStartProcess: Boolean = runningProcesses.get().length < MAX_RUNNING

  def addRunningProcess(cmd: String, userIp: String, processNum: Int): Unit = {
    var updated = false
    while (!updated) {
      val current = runningProcesses.get()
      val newList = current :+ ((cmd, userIp, processNum))
      updated = runningProcesses.compareAndSet(current, newList)
    }
  }

  def removeRunningProcess(processNum: Int): Unit = {
    var updated = false
    while (!updated) {
      val current = runningProcesses.get()
      val index = current.indexWhere(_._3 == processNum)
      if (index >= 0) {
        val newList = current.take(index) ++ current.drop(index + 1)
        updated = runningProcesses.compareAndSet(current, newList)
      } else {
        updated = true 
      }
    }
  }

  def enqueueCommand(cmd: String, userIp: String): Unit = {
    var updated = false
    while (!updated) {
      val current = pendingQueue.get()
      val newQueue = current :+ ((cmd, userIp)) 
      updated = pendingQueue.compareAndSet(current, newQueue)
    }
  }

  def dequeueCommand: Option[(String, String)] = {
    var result: Option[(String, String)] = None
    var updated = false
    while (!updated) {
      val current = pendingQueue.get()
      if (current.nonEmpty) {
        val currentQueue = current.clone() // Clone to avoid mutating during dequeue
        val element = currentQueue.dequeue() // Dequeue mutates, so use clone
        val newQueue = currentQueue
        updated = pendingQueue.compareAndSet(current, newQueue)
        if (updated) result = Some(element)
      } else {
        updated = true 
        result = None
      }
    }
    result
  }

  def toHtml: String = {
    val running = runningProcesses.get().map { case (cmd, ip, num) =>
      s"Process $num: $cmd (User: $ip)"
    }.mkString("<li>", "</li><li>", "</li>")
    val pending = pendingQueue.get().map { case (cmd, ip) =>
      s"$cmd (User: $ip)"
    }.mkString("<li>", "</li><li>", "</li>")
    s"""
      <p><strong>Total Requests:</strong> ${counter.get()}</p>
      <p><strong>Running Processes (max $MAX_RUNNING):</strong></p>
      <ul>$running</ul>
      <p><strong>Pending Commands:</strong></p>
      <ul>$pending</ul>
    """
  }
}