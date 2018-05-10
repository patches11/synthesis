package org.synthesis.design

import java.awt.Color

import org.synthesis.{Mixer, MixerChannel, Point, Wall}

import scala.util.Random

/**
  * Created by Brown on 6/19/17.
  */
case class SpaceFillingCurves(wall: Wall, orders: Seq[Int] = Seq(2, 4, 8, 16, 32)) extends Design {

  private val canvas = Array.ofDim[Int](wall.pixelsX,wall.pixelsY)

  for(x <- 0 until wall.pixelsX;
      y <- 0 until wall.pixelsY) yield {
    canvas(x)(y) = 0
  }

  def hilbert(n: Int, d: Int): Point = {
    Iterator.iterate(1)(_ * 2).
      takeWhile(_ < n).
      foldLeft(
        (d, Point(0, 0)))
    { case ((t: Int, point: Point), s: Int) =>
      val nx = 1 & (t/2)
      val ny = 1 & (t ^ nx)

      val npa = rot(s, point, nx, ny)
      val npb = Point(npa.x + s*nx, npa.y + s*ny)

      (t / 4, npb)
    }._2
  }

  def rot(n: Int, p: Point, rx: Int, ry: Int): Point = {
    if (ry == 0) {
      if (rx == 1) {
        Point(n-1 - p.x, n-1 - p.y).swap
      } else {
        p.swap
      }
    } else {
      p
    }
  }

  private val curves = orders.map { order =>
    val max = order * order

    0 until max map (i => hilbert(order, i))
  }

  var curveStep: Double = 0.0
  var curve = 0

  val fadeRate = 0.05f

  override def render(channel: MixerChannel): Unit = {
    val touched = Array.ofDim[Boolean](wall.pixelsX,wall.pixelsY)

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      touched(x)(y) = false
    }

    val currentOrder = orders(curve)
    val currentCurve = curves(curve)
    val currentScale = wall.pixelsX / currentOrder
    val currentMax = currentOrder * currentOrder
    val currentStep = curveStep.toInt

    import org.synthesis.Utils._
    val paletteShift = ((System.currentTimeMillis() % 100000) / 10.0).toInt

    val offsetX = (wall.pixelsX - currentOrder * currentScale) / 2
    val offsetY = (wall.pixelsY - currentOrder * currentScale) / 2

    0 until Math.min(currentStep, currentMax) foreach { i =>
      val point = currentCurve(i)

      currentScale.times { x: Int =>
        currentScale.times { y: Int =>
          val pointActual = Point(point.x * currentScale + x + offsetX, point.y * currentScale + y + offsetY)

          if (pointActual.inside(wall)) {
            canvas(pointActual.x)(pointActual.y) = Color.getHSBColor((paletteShift + i * currentScale) / 256.0f, 1.0f, 1.0f).getRGB
            touched(pointActual.x)(pointActual.y) = true
          }
        }
      }

    }

    curveStep += 1.0 / currentScale

    if (curveStep.toInt > currentMax) {
      curveStep = 0
      curve = (curve + 1) % orders.length
    }

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      if (touched(x)(y)) {
        channel.setPixel(x, y, canvas(x)(y))
      } else {
        // fade
        val (r, g, b) = Mixer.getComponents(channel.getPixel(x, y))
        val hsb = Color.RGBtoHSB(r, g, b, null)
        channel.setPixel(x, y, Color.getHSBColor(hsb(0), Math.max(hsb(1) - fadeRate, 0.0f), Math.max(hsb(2) - fadeRate, 0.0f)).getRGB)
      }
    }
  }
}
