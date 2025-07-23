package cp.serverPr

import scala.collection.mutable.{ListBuffer, Queue}

class ServerState() {
  private val MAX_RUNNING = 3                                         // Max running processes CHANGE AS NEEDED
  private var counter = 0                                             // Shared state: total requests counter
  private val runningProcesses = ListBuffer[(String, String, Int)]()  // Shared state: running processes tracker
  private val pendingQueue = Queue[(String, String)]()                // Shared state: pending commands queue

  def incrementAndGetCounter: Int = this.synchronized {
    counter += 1
    counter
  }

  def canStartProcess: Boolean = this.synchronized {
    runningProcesses.length < MAX_RUNNING
  }
 
  def addRunningProcess(cmd: String, userIp: String, processNum: Int): Unit = this.synchronized {
    runningProcesses += ((cmd, userIp, processNum))
    () 
  }

  def removeRunningProcess(processNum: Int): Unit = this.synchronized {
    val index = runningProcesses.indexWhere(_._3 == processNum)
    if (index >= 0) runningProcesses.remove(index)
    ()
  }

  def enqueueCommand(cmd: String, userIp: String): Unit = this.synchronized {
    pendingQueue.enqueue((cmd, userIp))
    ()
  }

  def dequeueCommand: Option[(String, String)] = this.synchronized {
    if (pendingQueue.nonEmpty) Some(pendingQueue.dequeue()) else None
  }

  // Server state HTML
  def toHtml: String = this.synchronized {
    val running = runningProcesses.map { case (cmd, ip, num) =>
      s"Process $num: $cmd (User: $ip)"
    }.mkString("<li>", "</li><li>", "</li>")
    val pending = pendingQueue.map { case (cmd, ip) =>
      s"$cmd (User: $ip)"
    }.mkString("<li>", "</li><li>", "</li>")
    s"""
      <p><strong>Total Requests:</strong> $counter</p>
      <p><strong>Running Processes (max $MAX_RUNNING):</strong></p>
      <ul>$running</ul>
      <p><strong>Pending Commands:</strong></p>
      <ul>$pending</ul>
    """
  }
}