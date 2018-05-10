package org.synthesis.design

import java.awt.Color

import org.synthesis.{MixerChannel, Point, Wall}

import scala.util.Random

/**
  * Created by Brown on 6/19/17.
  */
case class OneSpaceFillingCurve(wall: Wall, order: Int, fillSides: Boolean = false) extends Design {

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

  val max = order * order

  var step = 0

  val curve = 0 until max map(i => hilbert(order, i))

  val scale = wall.pixelsY / order

  override def render(channel: MixerChannel): Unit = {
    import org.synthesis.Utils._
    val paletteShift = ((System.currentTimeMillis() % 100000) / 10.0).toInt

    val offsetX = (wall.pixelsX - order * scale) / 2
    val offsetY = (wall.pixelsY - order * scale) / 2

    0 until Math.min(step, max) foreach { i =>
      val point = curve(i % max)


      scale.times { x: Int =>
        scale.times { y: Int =>
          val pointActual = Point(point.x * scale + x + offsetX, point.y * scale + y + offsetY)

          if (fillSides) {
            if (point.x == 0) {
              (0 until offsetX).foreach(x => channel.setPixel(x, pointActual.y, Color.getHSBColor((paletteShift + i) / 256.0f, 1.0f, 1.0f).getRGB))
            } else if (point.x == order - 1) {
              (1 to offsetX).foreach(x => channel.setPixel(pointActual.x + x, pointActual.y, Color.getHSBColor((paletteShift + i) / 256.0f, 1.0f, 1.0f).getRGB))
            }

            if (point.y == 0) {
              (0 until offsetY).foreach(y => channel.setPixel(pointActual.x, y, Color.getHSBColor((paletteShift + i) / 256.0f, 1.0f, 1.0f).getRGB))
            } else if (point.y == order - 1) {
              (1 to offsetY).foreach(y => channel.setPixel(pointActual.x, pointActual.y + y, Color.getHSBColor((paletteShift + i) / 256.0f, 1.0f, 1.0f).getRGB))
            }
          }

          if (pointActual.inside(wall)) {
            channel.setPixel(pointActual, Color.getHSBColor((paletteShift + i) / 256.0f, 1.0f, 1.0f).getRGB)
          }
        }
      }


    }

    step += 1
  }
}
