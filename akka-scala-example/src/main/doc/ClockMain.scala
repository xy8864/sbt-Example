import akka.actor._
import concurrent.duration._

sealed trait Message
case class ReturnInfluenceMessage(source: ActorRef) extends Message
case class SetInfluences(source: ActorRef) extends Message
case class Start(period:Int) extends Message
case object GetInfluence extends Message
case object Stop extends Message
case object TickMessage extends Message

class Listener extends Actor {
    def receive = {
        case _ =>
            println("Listener: received something\n")
    }
}

class Entity extends Actor {
    val router = context.actorOf(Props[Router], name = "Router")

    def receive = {
        case rim @ ReturnInfluenceMessage(source) =>
            source ! rim

        case msg =>
            shadow ! msg
    }
}

class Router extends Actor {
    val pulser = context.actorOf(Props[Pulser], name = "Pulser")
    //more children

    def receive = {
        case rim @ ReturnInfluenceMessage(source) =>
            context.parent ! rim

        case msg =>
            context.children foreach (_.forward(msg))
    }
}

class Pulser extends Actor {
    val clock = context.actorOf(Props[Clock], name = "Clock")

    def receive = {
        case TickMessage =>
            println ("Emission_Unit: received Tickmessage!\n")

        case _  =>
            println ("Emission_Unit: received something\n")
    }
}

class Clock extends Actor {
    def receive = ready

    def ready:Receive = {
        case Start(period) => {
            println ("Heartbeat: Start")
            context.become(running(period) orElse ready)
            context.system.scheduler.scheduleOnce(period milliseconds, self, TickMessage)
        }
    }

    def running(period:Int):Receive = {
        case TickMessage => {
            context.parent ! TickMessage
            context.system.scheduler.scheduleOnce(period milliseconds, self, TickMessage)
        }

        case Stop =>
            println("Clock: stopping")
            context.unbecome()
    }
}

object Main extends App {
    val system = akka.actor.ActorSystem("mySystem")
    val abel = system.actorOf(Props[Listener], name = "Listener")
    val cain = system.actorOf(Props[Entity], name = "Entity")

    import system.dispatcher
    //system.scheduler.scheduleOnce(...)
    //do something irrelevant
}
