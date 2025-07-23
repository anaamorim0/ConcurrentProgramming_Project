package abp

import akka.actor._
import org.slf4j.LoggerFactory
import scala.util.Random

class TransActor(modo: String) extends Actor {
    val log = LoggerFactory.getLogger(getClass)
    val random = new Random()

    def receive: Actor.Receive = {
        case msg: Mensagem =>
            log.info(s"[Trans] A processar a mensagem ${msg.valor} com bit ${msg.bit}")

            modo match {
                case "1" =>
                    context.actorSelection("/user/receiverActor") ! msg

                case "2" =>
                    // geramos um numero aleatorio, para decidir se a mensagem é enviada c sucesso, perdida ou duplicada
                    val s = random.nextInt(100)
                    if (s < 10) {
                        log.info(s"[Trans] Mensagem ${msg.valor} perdida")
                    }
                    else {
                        context.actorSelection("/user/receiverActor") ! msg
                        if (s > 60) {
                            log.info(s"[Trans] Mensagem ${msg.valor} duplicada, a enviar 2 vezes")
                            context.actorSelection("/user/receiverActor") ! msg 
                        }
                    }
                
                case "3" =>
                    // geramos um numero aleatorio, para decidir se a mensagem é enviada c sucesso, perdida ou duplicada
                    val s = random.nextInt(100)
                    // probabilidade de ser perdida muito alta
                    if (s < 99) {
                        log.info(s"[Trans] Mensagem ${msg.valor} perdida")
                    }
                    else {
                        context.actorSelection("/user/receiverActor") ! msg 
                        if (s > 99) {
                            log.info(s"[Trans] Mensagem ${msg.valor} duplicada, a enviar 2 vezes")
                            Thread.sleep(300)
                            context.actorSelection("/user/receiverActor") ! msg
                        }
                    }
            }
    }
}