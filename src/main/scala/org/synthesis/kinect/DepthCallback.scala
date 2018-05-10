package org.synthesis.kinect

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import org.openkinect.freenect.{DepthHandler, FrameMode}

class DepthCallback(system: ActorSystem) extends DepthHandler {

    val buffer: Array[Int] = Array.ofDim(KinectMaster.DEPTH_FRAME_SIZE)

    var frameNumber = 0
    val writeFile = false

    override def onFrameReceived(frameMode: FrameMode, byteBuffer: ByteBuffer, timestamp: Int): Unit = {
      if (byteBuffer != null) {

        var ix = 0
        var i = 0
        while (i < frameMode.getFrameSize) {
          val lo: Int = byteBuffer.get(i) & 255
          i += 1
          val hi = byteBuffer.get(i) & 255
          i += 1
          val sample = hi << 8 | lo

          buffer(ix) = sample
          ix += 1
        }

        system.eventStream.publish(DepthFrame(buffer, frameMode.width, frameMode.height))

        //debug()

      } else {
        println("null buffer")
      }
    }

    var debugCount = 1

    private def debug(): Unit = {
      val twoD: Array[Array[Double]] = Array.ofDim(KinectMaster.DEPTH_HEIGHT, KinectMaster.DEPTH_WIDTH)

      for(x <- 0 until KinectMaster.DEPTH_WIDTH;
          y <- 0 until KinectMaster.DEPTH_HEIGHT) yield {
        twoD(y)(x) = buffer(x + y * KinectMaster.DEPTH_WIDTH)
      }

      debugCount = debugCount + 1

      println(s"\n\n\nDebug Count $debugCount\n\n\n\n\n")

      if (writeFile && debugCount % 20 == 0) {
        import java.io.BufferedWriter
        import java.io.FileWriter
        val outputWriter = new BufferedWriter(new FileWriter("frame" + frameNumber + ".csv"))

        frameNumber = frameNumber + 1

        for(y <- 0 until KinectMaster.DEPTH_HEIGHT) yield {
          outputWriter.write(twoD(y).mkString(", "))
          outputWriter.newLine()
        }

        outputWriter.flush()
        outputWriter.close()
      }


      println("Debug Depth Info")
      for(y <- 0 until KinectMaster.DEPTH_HEIGHT) yield {
        //println(twoD(y).map(d => f"$d%1.2f").mkString(" "))
      }
    }

  }
