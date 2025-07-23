package abp

import akka.actor._
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext 

class SenderActor(numMensagens: Int) extends Actor {
    var bit = 0         // alterna entre 0 e 1
    var contador = 0    // n de mensagens enviadas até ao momento 
    var tentativas = 0      // quantas vezes foi enviada a mensagem
    val limiteTentativas = 5 // tenta reenviar ate 5 vezes antes de desistir
    var timeout = false

    val log = LoggerFactory.getLogger(getClass)

    implicit val ex: ExecutionContext = context.dispatcher

    // Inicia um timeout de 15 segundos para aguardar resposta
    def iniciarTimeout(): Unit = {
        if (!timeout) {
            context.system.scheduler.scheduleOnce(15.seconds, self, Timeout)(ex)
        }
    }

    def receive: Actor.Receive = {
        case Iniciar =>
            // enquanto houver mensagens para enviar
            if (contador < numMensagens) {
                // e ainda nao se atingiu o numero maximo de tentativas de reenvio
                if (tentativas < limiteTentativas) {
                    val mensagem = Mensagem(contador + 1, bit)  // Cria-se uma nova mensagem
                    log.info(s"[Sender] A enviar mensagem ${contador + 1} com bit $bit")
                    context.actorSelection("/user/transActor") ! mensagem   // Envio da mensagem para o Trans
                    iniciarTimeout()
                }
                // se se atingir o limite de tentativas de reenvio
                else {
                    log.info("Limite de tentativas de reenvio atingido. Falha no envio. A fechar o sistema.")
                    context.system.terminate()
                }
            }
        
        case Ack(bitRecebido) =>
            // se o ack recebido corresponde ao bit esperado
            if (bitRecebido == bit) {
                bit = 1 - bit   // alterna o bit
                contador += 1
                timeout = true  // cancela o timeout, porque o ack foi recebido
                Thread.sleep(500)
                log.info("")

                if (contador < numMensagens) {
                    self ! Iniciar
                }
                else {
                    log.info("Transmissao concluida. A encerrar o sistema")
                    context.system.terminate()
                }
            }

        case Timeout =>
            // se nao recebeu ack dentro do tempo limite
            if (tentativas < limiteTentativas) {
                tentativas += 1
                timeout = false     // reset do timeout
                log.info(s"Nenhuma resposta por 15 segundos. A reenviar mensagem ${contador + 1}")
                self ! Iniciar      // reenvia a mensagem
            }
            else {
                context.system.terminate()
            }
    }
}

case object Timeout