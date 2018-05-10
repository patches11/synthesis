package org.synthesis.kinect

import akka.actor.ActorSystem
import org.openkinect.freenect._

class KinectMaster(system: ActorSystem) {
  private val ctx = Freenect.createContext
  private val device = ctx.openDevice(0)

  device.setLed(LedStatus.OFF)

  device.setDepthFormat(DepthFormat.REGISTERED)
  device.startDepth(new DepthCallback(system))

  //device.setVideoFormat(VideoFormat.RGB)//, Resolution.LOW)
  //device.startVideo(new VideoCallback(system))

  sys.addShutdownHook(device.stopDepth())
  //sys.addShutdownHook(device.stopVideo())
}

object KinectMaster {
  val DEPTH_HEIGHT = 480
  val DEPTH_WIDTH = 640

  val DEPTH_FRAME_SIZE: Int = DEPTH_WIDTH * DEPTH_HEIGHT

  val depthFormat = DepthFormat.MM
}

case class DepthFrame(twoD: Array[Int], width: Int, height: Int)

