package cp.serverPr

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.slf4j.LoggerFactory
import scala.sys.process._

object Routes {
  private val logger = LoggerFactory.getLogger(getClass)
  private val state = new ServerState()

  val routes: IO[HttpRoutes[IO]] = IO {
    HttpRoutes.of[IO] {
      // "status" request
      case GET -> Root / "status" =>
        Ok(state.toHtml)
          .map(addCORSHeaders)
          .map(_.withContentType(org.http4s.headers.`Content-Type`(MediaType.text.html)))

      // "run-process" request
      case req @ GET -> Root / "run-process" =>
        val cmdOpt = req.uri.query.params.get("cmd")
        val userIp = req.remoteAddr.map(_.toString).getOrElse("unknown")

        cmdOpt match {
          case Some(cmd) =>
            if (state.canStartProcess) {
              val processNum = state.incrementAndGetCounter
              state.addRunningProcess(cmd, userIp, processNum)
              // Run process asynchronously
              runProcess(cmd, userIp, processNum).flatMap { output =>
                Ok(s"Started process [$processNum] for $cmd: $output")
                  .map(addCORSHeaders)
              }
            } else {
              state.enqueueCommand(cmd, userIp)
              Ok(s"Process queued for $cmd")
                .map(addCORSHeaders)
            }

          case None =>
            BadRequest("⚠️ Command not provided. Use /run-process?cmd=<your_command>")
              .map(addCORSHeaders)
        }
    }
  }

  /** Run a given process and collect its output. */
  private def runProcess(cmd: String, userIp: String, processNum: Int): IO[String] = {
    IO {
      logger.info(s"🔹 Starting process ($processNum) for user $userIp: $cmd")
      // Execute command
      val output = new StringBuilder
      val error = new StringBuilder
      val processLogger = ProcessLogger(
        (o: String) => { output.append(o).append("\n"); () },
        (e: String) => { error.append(e).append("\n"); () }
      )
      try {
        val exitCode = Process(Seq("bash", "-c", cmd)).!(processLogger)
        val result = if (exitCode == 0) {
          s"[$processNum] Result from running $cmd for user $userIp: ${output.toString.trim}"
        } else {
          s"[$processNum] Failed running $cmd for user $userIp (exit code $exitCode): ${error.toString.trim}"
        }
        state.removeRunningProcess(processNum)
        result
      } catch {
        case e: Exception =>
          val errorMsg = s"[$processNum] Error running $cmd for user $userIp: ${e.getMessage}"
          logger.error(errorMsg, e)
          state.removeRunningProcess(processNum)
          errorMsg
      }
    }.flatMap { output =>
      state.dequeueCommand match {
        case Some((nextCmd, nextIp)) =>
          val newProcessNum = state.incrementAndGetCounter
          state.addRunningProcess(nextCmd, nextIp, newProcessNum)
          // Start process asynchronously
          runProcess(nextCmd, nextIp, newProcessNum).start.map(_ => output)
        case None =>
          IO.pure(output)
      }
    }
  }

  /** Add extra headers */
  def addCORSHeaders(response: Response[IO]): Response[IO] = {
    response.putHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Content-Type, Authorization",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }
}