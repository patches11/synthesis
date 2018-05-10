package org.synthesis.kinect

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import org.opencv.core.{CvType, Mat}
import org.openkinect.freenect.{FrameMode, VideoHandler}
import org.synthesis.webcam.WebcamFrame

class VideoCallback(system: ActorSystem)  extends VideoHandler {
  override def onFrameReceived(frameMode: FrameMode, byteBuffer: ByteBuffer, i: Int) = {
    if (byteBuffer != null) {

      var ix = 0
      var i = 0
      val frame: Mat = new Mat(frameMode.height, frameMode.width, CvType.CV_8UC3)
      while (i < frameMode.getFrameSize) {
        val r = byteBuffer.get(i)
        i += 1
        val g = byteBuffer.get(i)
        i += 1
        val b = byteBuffer.get(i)
        i += 1


        val color: Byte = 255.toByte

        val y = ix / frameMode.width
        val x = ix - y * frameMode.width

        frame.put(y, x, Array(r, g, b))

        ix += 1
      }

      system.eventStream.publish(WebcamFrame(frame))

      //debug()

    } else {
      println("null buffer")
    }
  }
}
