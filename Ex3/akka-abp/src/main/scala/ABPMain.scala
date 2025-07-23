package abp

import akka.actor._
import org.slf4j.LoggerFactory
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn.readLine

object ABPMain extends App {
    lazy val sistema = akka.actor.ActorSystem("ABP")
    val log = LoggerFactory.getLogger(getClass)

    log.info("Protocolo ABP iniciado")

    // o user pode escolher quantas mensagens deseja enviar
    log.info("Quantas mensagens pretende enviar?")
    val total = readLine("Numero: ").toIntOption.getOrElse(3)

    // o user pode escolher o modo que quer que sejam processadas as mensagens
    log.info("Por favor, escolha um modo:")
    log.info("1 - Comunicacao com sucesso (sem perda, nem duplicacao)")
    log.info("2 - Comunicacao com algumas falhas (mensagens perdidas e duplicadas mas todas entregues)")
    log.info("3 - Comunicacao com falhas graves (mensagens nao entregues)")

    val escolha = readLine("Modo: ").toIntOption.getOrElse(1)
    val modo = escolha match {
        case 1 => "1"
        case 2 => "2"
        case 3 => "3"
        case _ => "1"
    }

    val senderActor = sistema.actorOf(Props(new SenderActor(total)), "senderActor")
    val transActor = sistema.actorOf(Props(new TransActor(modo)), "transActor")
    val receiverActor = sistema.actorOf(Props[ReceiverActor], "receiverActor")
    val ackActor = sistema.actorOf(Props[AckActor], "ackActor")

    senderActor ! Iniciar 

    Await.result(sistema.whenTerminated, Duration.Inf)
}