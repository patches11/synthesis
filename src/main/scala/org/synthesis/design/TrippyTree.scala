package org.synthesis.design

import org.synthesis
import org.synthesis.design.support.Reset
import org.synthesis.{MixerChannel, Wall}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  *
  * Created by patrickbrown on 5/31/17.
  *
  */


case class TrippyTree(wall: Wall) extends Design {
  private lazy val pallet = newPallet()
  val segments: mutable.ArrayBuffer[Segment] = ArrayBuffer()
  val segmentProgress: mutable.ArrayBuffer[Double] = ArrayBuffer()
  private val initX = wall.pixelsX / 2
  private val initY = wall.pixelsY - 1

  private var origin: (Double, Double) = (initX, initY)

  this.reset
  private val canvas = Array.ofDim[Int](wall.pixelsX, wall.pixelsY)
  private var last = System.currentTimeMillis()
  private val ratio = 60.0 / 100000
  private var waitTime: Double = 0
  private val waitLimit = 5
  private val hayflickLimit = 5
  private var angleCoefficient = Math.PI
  private var currentAngle: Double = 0.0
  private val rotate = true

  for (x <- 0 until wall.pixelsX;
       y <- 0 until wall.pixelsY) yield {
    canvas(x)(y) = 0
  }

  override def support: Receive = {
    case Reset => reset
  }

  override def render(channel: MixerChannel): Unit = {
    import org.synthesis.Utils._

    val paletteShift = System.currentTimeMillis() / 500
    val step = (System.currentTimeMillis() - last) * ratio
    last = System.currentTimeMillis()

    currentAngle = (Math.PI / 16) * Math.sin(paletteShift / 5)

    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      canvas(x)(y) = 0
    }

    segmentProgress.zipWithIndex.foreach { case (progress, index) =>
      var newProgress = progress + step
      if (newProgress > 1.0) {
        newProgress = 1.0
      }

      segmentProgress(index) = newProgress

      val segment = segments(index)
      val base = if (segment.generation == 0 || segment.generation == 1) 1 else 0

      renderLine(segment, newProgress, pallet(((paletteShift - segment.generation) % pallet.length).toInt))

      if (progress < 1.0 && newProgress >= 1.0 && segment.generation < hayflickLimit) {
        (base + Random.nextInt(5)).times { _: Int =>
          val (x, y) = getPoint(segment, newProgress, false)
          segments += Segment(x, y, Random.nextInt(wall.pixelsY / 2 / (segment.generation + 3)) + wall.pixelsY / 10, newAngle(segment.generation), segment.generation + 1)
          segmentProgress += 0.0
        }
      }
    }

    if (segmentProgress.forall(_ >= 1.0)) {
      waitTime += step
      if (waitTime > waitLimit) {
        this.reset
      }
    }

    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, canvas(x)(y))
    }
  }

  def newAngle(gen: Int): Double = {
    Random.nextDouble() * (angleCoefficient * (gen + 1) / 2) + Math.PI + (angleCoefficient * (gen + 1) / 4)
  }

  def reset: Unit = {
    segments.clear()
    segmentProgress.clear()
    segments += Segment(initX, initY, Random.nextInt(wall.pixelsY / 10) + wall.pixelsY / 5, Random.nextDouble() * Math.PI / 2 + Math.PI + Math.PI / 4, 0)
    origin = getPoint(segments.head, 1.0, false)
    segmentProgress += 0.0
    waitTime = 0
    angleCoefficient = Random.nextDouble * Math.PI / 2
  }

  def renderLine(segment: Segment, progress: Double, color: Int): Unit = {
    val (desX, desY) = getPoint(segment, progress, rotate && segment.generation != 0)
    val (sx, sy) = segment.getXY(origin, currentAngle, rotate && segment.generation != 0)
    val pixels = bresenham(sx.toInt, sy.toInt, desX.toInt, desY.toInt).toList
    pixels.zipWithIndex.foreach { case ((x, y), index) =>
      if (x >= 0 && y >= 0 && x < wall.pixelsX && y < wall.pixelsY) {
        canvas(x)(y) = color
      }
    }
  }

  def getPoint(segment: Segment, progress: Double, rot: Boolean): (Double, Double) = {
    val newX = segment.x + Math.cos(segment.direction) * segment.length * progress
    val newY = segment.y + Math.sin(segment.direction) * segment.length * progress
    if (rot) {
      synthesis.Utils.rotate((newX, newY), origin, currentAngle)
    } else {
      (newX, newY)
    }
  }

  def bresenham(x0: Int, y0: Int, x1: Int, y1: Int): Iterator[(Int, Int)] = {
    import scala.math.abs

    val dx = abs(x1 - x0)
    val dy = abs(y1 - y0)

    val sx = if (x0 < x1) 1 else -1
    val sy = if (y0 < y1) 1 else -1

    new Iterator[(Int, Int)] {
      var (x, y) = (x0, y0)
      var err = dx - dy

      def next = {
        val point = (x, y)
        val e2 = 2 * err
        if (e2 > -dy) {
          err -= dy
          x += sx
        }
        if (e2 < dx) {
          err += dx
          y += sy
        }
        point
      }

      def hasNext = !(x == x1 && y == y1)
    }
  }

  private def newPallet() = {
    import java.awt.Color
    (0 to (Random.nextInt(5) + 3)).map(_ =>
      Color.getHSBColor(Random.nextFloat, Random.nextFloat, 1.0f).getRGB
    )
  }
}


case class Segment(x: Double, y: Double, length: Double, direction: Double, generation: Int) {
  def getXY(around: (Double, Double), angle: Double, rotate: Boolean): (Double, Double) = {
    if (rotate) {
      synthesis.Utils.rotate((x, y), around, angle)
    } else {
      (x, y)
    }
  }
}