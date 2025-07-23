package abp 

import akka.actor._
import org.slf4j.LoggerFactory

class AckActor extends Actor {
    val log = LoggerFactory.getLogger(getClass)

    def receive: Actor.Receive = {
        case Ack(bit) =>
            Thread.sleep(300)
            log.info(s"[Ack] Recebido ack para bit $bit")
            context.actorSelection("/user/senderActor") ! Ack(bit)
            log.info(s"[Ack] A enviar ack para bit $bit")
    }
}