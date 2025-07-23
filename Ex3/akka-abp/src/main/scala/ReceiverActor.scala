package abp 

import akka.actor._
import org.slf4j.LoggerFactory

class ReceiverActor extends Actor {
    var bit = 0
    val log = LoggerFactory.getLogger(getClass)

    def receive: Actor.Receive = {
        case Mensagem(m, b) =>
            log.info(s"[Receiver] Recebeu mensagem $m com bit $b")

            // apenas processa se o bit for o esperado
            if (bit == b ) {
                Thread.sleep(300)
                context.actorSelection("/user/ackActor") ! Ack(b)
                bit = 1 - bit
            }
    }
}