package org.synthesis.controller

import akka.actor.ActorRef

/**
  * Created by Brown on 7/12/17.
  */
case class DesignAndController(design: ActorRef, controller: Controller = DefaultController, name: String = "")
