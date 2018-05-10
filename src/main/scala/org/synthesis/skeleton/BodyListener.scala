package org.synthesis.skeleton

import akka.actor.Actor

class BodyListener extends Actor {
  context.system.eventStream.subscribe(self, classOf[Body])

  override def receive: Receive = {
    case b: Body =>
      println(s"received body: $b")
  }
}
