package org.synthesis.controller

import akka.actor.ActorRef

/**
  * Created by Brown on 7/12/17.
  */
object Implicits {

  implicit def actorRefToDesignAndController(x: ActorRef): DesignAndController =
    DesignAndController(x, DefaultController)


  implicit def actorRefsToDesignAndController(x: Seq[ActorRef]): Seq[DesignAndController] =
    x.map(d => DesignAndController(d, DefaultController))

}
