package org.synthesis.kinect

import java.awt.{Color, Graphics}
import java.awt.image.BufferedImage
import javax.swing.JPanel

import akka.actor.{Actor, ActorSystem, Props}
import org.synthesis.skeleton._

class DepthPanel(system: ActorSystem, fps: Int) extends JPanel with Runnable {
  var currentFrame: BufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
  var bodies: List[Body] = Nil
  var started = false
  private val fpses = scala.collection.mutable.ListBuffer[Double]()
  var iterationCount: Long = 0

  val kwidth = 640
  val kheight = 480

  protected override def paintComponent(g: Graphics) {
    val t0 = System.nanoTime()
    super.paintComponent(g)
    iterationCount += 1

    if (started) {
      g.drawImage(currentFrame, 0, 0, null)

      bodies.foreach { body =>
        body.allPoints.foreach { point =>
          val color = point.kind match {
            case InterestPoint.Center =>
              Color.WHITE
            case InterestPoint.Head =>
              Color.GREEN
            case InterestPoint.LeftHand =>
              Color.BLUE
            case InterestPoint.RightHand =>
              Color.CYAN
            case InterestPoint.LeftFoot =>
              Color.MAGENTA
            case InterestPoint.RightFoot =>
              Color.ORANGE
            case _ =>
              Color.RED
          }
          g.setColor(color)
          g.fillOval(point.x, point.y, 20, 20)
        }
      }
    }

    g.dispose()
    fpses += (1000000000 / (System.nanoTime() - t0))
    if (iterationCount % 120 == 0) {
      System.out.print(f"Depth Panel fps ${"%.1f".format(fpses.sum / fpses.size)} \r")
      fpses.clear()
    }
  }

  class DepthListener extends Actor {

    var itCount = 0L

    context.system.eventStream.subscribe(self, classOf[DepthFrame])

    def receive = {
      case DepthFrame(frame, width, _) =>
        for(x <- 0 until currentFrame.getWidth;
            y <- 0 until currentFrame.getHeight) yield {

          val depth = frame(x + y * width)

          currentFrame.setRGB(x, y, Color.HSBtoRGB(0.75f, 1.0f, depth.toFloat / 8000))
        }
        started = true
      itCount += 1
    }
  }

  class BodyListener extends Actor {
    context.system.eventStream.subscribe(self, classOf[BodyList])

    var itCount = 0L

    def receive = {
      case BodyList(bs) =>
        if (itCount % 120 == 0) {
          println(s"interest points: ${bs.size}")
        }
        bodies = bs
        itCount += 1
    }
  }

  override def run() {

    currentFrame = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_ARGB)

    val depthListener = system.actorOf(Props(new DepthListener()))
    val bodyListener = system.actorOf(Props(new BodyListener()))

    while (true) {
      this.repaint()
      try {
        Thread.sleep(1000 / fps)
      }
      catch {
        case ex: InterruptedException => {}
      }
    }
  }
}