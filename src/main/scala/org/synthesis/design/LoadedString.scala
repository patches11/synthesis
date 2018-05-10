package org.synthesis.design

import akka.actor.{Actor, Props}
import breeze.linalg._
import org.synthesis.design.support.Reset
import org.synthesis.palette.Palette
import org.synthesis.skeleton.{Body, BodyList, InterestPoint}
import org.synthesis.util.Activate
import org.synthesis.{Mixer, MixerChannel, Wall}

import scala.collection.mutable
import scala.util.Random

case class LoadedString(wall: Wall, defaultPalette: Palette, listen: Boolean = true, random: Boolean = true) extends PaletteDesign {

  private val randomBubble = random
  private val canvas = Array.ofDim[(Float, Float, Float)](wall.pixelsX, wall.pixelsY)

  for (x <- 0 until wall.pixelsX;
       y <- 0 until wall.pixelsY) yield {
    canvas(x)(y) = (0, 0, 0)
  }

  val bubbleChance = 0.01
  val darkenCutoff = 0.02
  val bodyRange = 10
  val xMax = 640
  val yMax = 480
  val yMod = 10.0
  val yDiffMin: Double = 0.7
  val yDiffMax: Double = wall.pixelsY / 10
  val N = wall.pixelsX
  val T = 1.0
  val m = 1.0
  val a = 1.0
  val diffModifier = 3
  val dampening = 0.002
  val colorDampening = 0.01
  val omega = (0 until N).map(normalModeFreq)
  val as: mutable.ArrayBuffer[Double] = mutable.ArrayBuffer[Double]((0 until N).map(_ => 0.0): _*)
  val bs: mutable.ArrayBuffer[Double] = mutable.ArrayBuffer[Double]((0 until N).map(_ => 0.0): _*)

  val bodyListener = if (listen) Some(context.system.actorOf(Props(new BodyListener()))) else None
  val bubbleDistance = 5*1000

  private val midMinusFour = wall.pixelsX / 2 - 4
  var previousBodies = mutable.ArrayBuffer[(Int, Int, Double, Long)]()
  var lastBodyBubble = System.currentTimeMillis()

  reset()

  var offsetT = System.currentTimeMillis()
  protected var internalPalette = defaultPalette

  override def support: Receive = {
    case Reset => reset()
  }

  override def render(channel: MixerChannel): Unit = {
    if (iterationCount % 10 == 0) {
      bodyListener.foreach(b => b ! Activate)
    }

    if (randomBubble && System.currentTimeMillis() - lastBodyBubble > bubbleDistance && Random.nextDouble < bubbleChance) {
      lastBodyBubble = System.currentTimeMillis()
      createBubble()
    }

    val t = getT
    val colorT = System.currentTimeMillis()

    val positions = (0 until N).map { x =>
      (0 until N).map(normalModePos(_, x, t)).sum
    }

    (0 until N).foreach { j =>
      if (Math.abs(as(j)) > 0) {
        val sign = if (as(j) < 0) -1 else 1

        as(j) -= dampening * sign
      }

      if (Math.abs(bs(j)) > 0) {
        val sign = if (bs(j) < 0) -1 else 1

        bs(j) -= dampening * sign
      }
    }

    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      canvas(x)(y) = Mixer.darken(canvas(x)(y), colorDampening, darkenCutoff)
    }

    (0 until N).foreach { x =>
      val y = positions(x).toInt + wall.pixelsY / 2

      if (y >= 0 && y < wall.pixelsY) {
        canvas(x)(y) = Mixer.RGBtoHSB(internalPalette.getColor(((colorT - wall.pixelsX * x) % 10000).toInt))
      }
    }

    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, Mixer.HSBtoRGB(canvas(x)(y)))
    }
  }

  private def getT = {
    ((System.currentTimeMillis() - offsetT) / 50).toDouble
  }

  private def createBubble(center: Int = Random.nextInt(wall.pixelsX), r: Double = Random.nextInt(5)) = {

    def circleY(x: Int, center: Int, rr: Double): Double = {
      Math.sqrt(rr * rr - Math.pow(x - center, 2))
    }

    val dys = mutable.ArrayBuffer[Double]((0 until N).map(_ => 0.0): _*)

    val sign = if (r > 0) 1 else -1

    val rActual = Math.abs(r)

    val start = Math.max(0, center - rActual + 1)
    val end = Math.min(N - 1, center + rActual - 1)

    (start.toInt to end.toInt).foreach { x =>
      dys(x) = sign * circleY(x, center, rActual)
    }

    update(Array.fill(N)(0.0), dys.toArray)
  }

  private def update(dy: Array[Double], dvY: Array[Double] = Array.fill(N)(0.0), reset: Boolean = false): Unit = {
    val t = getT

    val ys = (0 until N).map { x =>
      (0 until N).map(normalModePos(_, x, t)).sum
    }

    val vys = (0 until N).map { x =>
      (0 until N).map(normalModeVel(_, x, t)).sum
    }

    val newYs = if (reset) {
      dy.toSeq
    } else {
      ys.zip(dy).map { case (a, b) => a + b }
    }

    val newYvs = if (reset) {
      dvY.toSeq
    } else {
      vys.zip(dvY).map { case (a, b) => a + b }
    }

    val aV = solveA(DenseVector(newYs: _*))

    val bV = solveB(DenseVector(newYvs: _*))

    (0 until N).foreach { j =>
      as(j) = aV(j)
      bs(j) = bV(j)
    }

    offsetT = System.currentTimeMillis()
  }

  private def solveA(positions: DenseVector[Double]): DenseVector[Double] = {
    val A = DenseMatrix((0 until N).map { n =>
      (0 until N).map(j => Math.sin((n + 1) * (j + 1) * Math.PI / (N + 1)))
    }: _*)

    A \ positions
  }


  def reset(): Unit = {
    val ys = mutable.ArrayBuffer[Double]((0 until N).map(_ => 0.0): _*)

    ys(midMinusFour) = - wall.pixelsY / 50
    ys(midMinusFour + 1) = - wall.pixelsY / 20
    ys(midMinusFour + 2) = - wall.pixelsY /  10
    ys(midMinusFour + 3) = - wall.pixelsY / 13
    ys(midMinusFour + 4) = - wall.pixelsY / 10
    ys(midMinusFour + 5) = - wall.pixelsY / 5
    ys(midMinusFour + 6) = - wall.pixelsY / 3
    ys(midMinusFour + 7) = - wall.pixelsY / 10
    ys(midMinusFour + 8) = - wall.pixelsY / 25

    update(ys.toArray, reset = true)
  }

  private def solveB(speeds: DenseVector[Double]): DenseVector[Double] = {
    val A = DenseMatrix((0 until N).map { n =>
      (0 until N).map(j => Math.sin((n + 1) * (j + 1) * Math.PI / (N + 1)) * omega(j))
    }: _*)

    A \ speeds
  }

  private def normalModePos(j: Int, n: Int, t: Double): Double = {
    Math.sin((n + 1) * (j + 1) * Math.PI / (N + 1)) * (as(j) * Math.cos(omega(j) * t) + bs(j) * Math.sin(omega(j) * t))
  }

  private def normalModeVel(j: Int, n: Int, t: Double): Double = {
    Math.sin((n + 1) * (j + 1) * Math.PI / (N + 1)) * omega(j) * (bs(j) * Math.cos(omega(j) * t) - as(j) * Math.sin(omega(j) * t))
  }

  private def normalModeFreq(j: Int): Double = {
    2 * Math.sqrt(T / (m * a)) * Math.sin((j + 1) * Math.PI / (2 * (N + 1)))
  }

  class BodyListener extends Actor {

    context.system.eventStream.subscribe(self, classOf[BodyList])

    val waiting: Receive = {
      case Activate =>
        context.become(receiving)
        lastActivate = System.currentTimeMillis()
    }
    private val waitTime = 5 * 1000
    private var lastActivate = System.currentTimeMillis()

    def receive = waiting

    def receiving: Receive = {
      case Activate =>
        lastActivate = System.currentTimeMillis()
      case BodyList(bodies) =>
        if (lastActivate < System.currentTimeMillis() - waitTime) {
          println("LoadedString - Becoming Waiting")
          context.become(waiting)
        } else {
          val allPoints = bodies.flatMap { newBody =>
            newBody.rightHand.toList ++ newBody.leftHand.toList
          }

          val p2 = allPoints.map { intPoint =>
            val old = previousBodies.find { case (x, lastY, size, lastCap) => intPoint.x >= x - bodyRange && intPoint.x <= x + bodyRange }

            old match {
              case Some(oldBody@(x, lastY, size, lastCap)) =>
                val yDiff = ((lastY - intPoint.y).toDouble / yMax) * yMod

                val newSize = yDiff * intPoint.depth.map(_.toDouble / 1000).getOrElse(1.0)

                val index = previousBodies.indexOf(oldBody)

                previousBodies.remove(index)

                (intPoint.x, intPoint.y, size + newSize, System.currentTimeMillis())
              case _ =>
                (intPoint.x, intPoint.y, 0.0, System.currentTimeMillis())
            }
          }

          val p3 = p2.flatMap {
            case (x, y, size, lastCap) =>
              if (Math.abs(size) > yDiffMin && Math.abs(size) < yDiffMax) {
                val xx = wall.pixelsX - ((x.toDouble / xMax) * wall.pixelsX).toInt - 1
                lastBodyBubble = System.currentTimeMillis()
                createBubble(xx, -size)
                Nil
              } else if (Math.abs(size) > yDiffMax) {
                Nil
              } else {
                Seq(p2)
              }
          }

          val bodiesRemoved = previousBodies.filter { case (x, y, size, lastCap) => System.currentTimeMillis() - lastCap > 5000 }

          previousBodies = bodiesRemoved ++ p2
        }

    }
  }
}
