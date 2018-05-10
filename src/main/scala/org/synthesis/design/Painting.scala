package org.synthesis.design

import java.awt.Color

import akka.actor.{Actor, Props}
import org.synthesis.design.support.Reset
import org.synthesis.skeleton.{Body, BodyList}
import org.synthesis.util.Activate
import org.synthesis.{Mixer, MixerChannel, Wall}

/**
  * Created by Brown on 6/9/17.
  */
case class Painting(wall: Wall) extends Design {
  val (width, height) = (640, 480)
  val (detectWidth, detectHeight) = (400, 400)
  val bodyListener = context.system.actorOf(Props(new BodyListener()))
  private val canvas = Array.ofDim[Int](wall.pixelsX, wall.pixelsY)

  private val maxLineDistance = 5
  private var currentColor: Int = Mixer.green
  private var lastLocs: List[(Int, Int)] = Nil

  reset()

  override def render(channel: MixerChannel): Unit = {
    if (iterationCount % 10 == 0) {
      bodyListener ! Activate
    }
    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, canvas(x)(y))
    }
  }

  private def gcd(a: Int, b: Int): Int = {
    if (b == 0) a else gcd(b, a % b)
  }

  private def draw(x: Int, y: Int): Unit = {
    if (x >= 0 && x < wall.pixelsX && y >= 0 && y < wall.pixelsY) {
      canvas(x)(y) = currentColor
    }
  }

  override def support: Receive = {
    case Reset =>
      reset()
  }

  private def reset(): Unit = {
    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      canvas(x)(y) = 0
    }
  }

  class BodyListener extends Actor {

    context.system.eventStream.subscribe(self, classOf[BodyList])

    val waiting: Receive = {
      case Activate =>
        context.become(painting)
        lastActivate = System.currentTimeMillis()
    }
    private val waitTime = 5 * 1000
    private var lastActivate = System.currentTimeMillis()

    def receive = waiting

    def painting: Receive = {
      case Activate =>
        lastActivate = System.currentTimeMillis()
      case BodyList(bodies) =>
        if (lastActivate < System.currentTimeMillis() - waitTime) {
          context.become(waiting)
        } else {
          val prevs = lastLocs
          lastLocs = Nil

          bodies.foreach { body =>
            body.rightHand match {
              case Some(rightHand) =>
                val adjustedX = (width - rightHand.x - 1) - 240
                val adjustedY = rightHand.y - 10

                if (adjustedX >= 0) {
                  if (adjustedY > 400) {
                    // Choose color
                    val adjustedY = 480 - rightHand.y
                    val nextColor = Color.HSBtoRGB(adjustedX.toFloat / 400, 1.0f, adjustedY.toFloat / 70)
                    currentColor = Mixer.blend(nextColor, currentColor, 0.9)
                  } else {
                    val (x, y) = ((adjustedX.toDouble / 390 * wall.pixelsX).toInt, (adjustedY.toDouble / 400 * wall.pixelsY).toInt)
                    lastLocs = lastLocs :+ (x, y)

                    val closePrevious = prevs.map { case t@(x2, y2) => (Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y)), t) }.sortBy(_._1).headOption

                    closePrevious match {
                      case Some((distance, (x2, y2))) if distance < maxLineDistance && distance > 0 =>
                        val (a, b) = (x2 - x, y2 - y)
                        val d = gcd(a, b)
                        val d2 = if (d == 0) 1 else d
                        val (slopeX, slopeY) = (a / d2, b / d2)

                        Iterator.iterate((x, y)) {
                          case (x3, y3) => (x3 + slopeX, y3 + slopeY)
                        }.takeWhile {
                          case (x3, y3) => (if (slopeX > 0) x3 <= x2 else x3 >= x2) && (if (slopeY > 0) y3 <= y2 else y3 >= y2)
                        }.foreach {
                          case (x3, y3) => draw(x3, y3)
                        }
                      case _ =>
                        draw(x, y)
                    }
                  }
                }
              case _ =>
            }
          }

        }
    }
  }
}
