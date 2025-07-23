package cp.serverPr

import scala.collection.mutable.{ListBuffer, Queue}


class ServerState {
  private val MAX_RUNNING = 3 // Max running processes CHANGE AS NEEDED
  @volatile private var counter = 0 // Volatile counter for visibility across threads
  @volatile private var runningProcesses = ListBuffer[(String, String, Int)]() // Volatile for visibility
  @volatile private var pendingQueue = Queue[(String, String)]() // Volatile for visibility

  // Get counter and increment with synchronization for atomicity
  def incrementAndGetCounter: Int = synchronized {
    counter += 1
    counter
  }

  // Check if a new process can start
  def canStartProcess: Boolean = synchronized {
    runningProcesses.length < MAX_RUNNING
  }

  // Add process to running list
  def addRunningProcess(cmd: String, userIp: String, processNum: Int): Unit = synchronized {
    runningProcesses = runningProcesses :+ ((cmd, userIp, processNum)) // Explicit tuple
  }

  // Remove process from running list
  def removeRunningProcess(processNum: Int): Unit = synchronized {
    val index = runningProcesses.indexWhere(_._3 == processNum)
    if (index >= 0) {
      runningProcesses = runningProcesses.take(index) ++ runningProcesses.drop(index + 1)
    }
  }

  // Enqueue command if limit reached
  def enqueueCommand(cmd: String, userIp: String): Unit = synchronized {
    pendingQueue = pendingQueue :+ ((cmd, userIp)) // Explicit tuple
  }

  // Dequeue command if available
  def dequeueCommand: Option[(String, String)] = synchronized {
    if (pendingQueue.nonEmpty) {
      val currentQueue = pendingQueue
      val element = currentQueue.dequeue()
      pendingQueue = currentQueue
      Some(element)
    } else {
      None
    }
  }

  // Server state HTML
  def toHtml: String = synchronized {
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