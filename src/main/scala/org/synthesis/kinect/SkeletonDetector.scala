package org.synthesis.kinect

import akka.actor.{Actor, ActorRef}
import org.synthesis.AfterTheLastMessage
import org.synthesis.skeleton.{BodyList, Skeleton}

class SkeletonDetector extends Actor {

  context.system.eventStream.subscribe(self, classOf[DepthFrame])

  private val skeleton = Skeleton(resample = 8, disThreshold = 3000, difThreshold = 100, poiCount = 4)

  var itCount = 0
  val fpsesF = scala.collection.mutable.ListBuffer[Double]()

  val waiting: Receive = {
    case w: DepthFrame =>
      self ! AfterTheLastMessage
      context.become(searchingForWork(w, sender))
  }

  def searchingForWork(lastWorkSoFar: DepthFrame, itsSender: ActorRef): Receive = {
    case AfterTheLastMessage =>
      processWork(lastWorkSoFar)
      context.become(waiting)
    case w: DepthFrame =>
      context.become(searchingForWork(w, sender))
  }

  def receive = waiting

  def processWork(depthFrame: DepthFrame): Unit = {
    itCount += 1
    val t0 = System.nanoTime()

    val bodies = skeleton.track(depthFrame.twoD, depthFrame.width, depthFrame.height)

    context.system.eventStream.publish(BodyList(bodies))

    fpsesF += (1000000000 / (System.nanoTime() - t0))
    if (itCount % 120 == 0) {
      System.out.print(s"Skeleton Detect fps ${"%.1f".format(fpsesF.sum / fpsesF.size)} \r")
      fpsesF.clear()
    }
  }
}
