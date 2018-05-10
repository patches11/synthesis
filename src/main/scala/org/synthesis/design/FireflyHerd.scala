package org.synthesis.design

import java.awt.Color

import org.synthesis.{MixerChannel, Wall}

import scala.util.Random

/**
  * Created by Brown on 6/19/17.
  */
case class FireflyHerd(wall: Wall) extends Design {
  val count = wall.pixelsX * wall.pixelsY / 10 + 50
  import org.synthesis.Utils.distance

  private val localBuffer = Array.ofDim[Int](wall.pixelsX,wall.pixelsY)

  private val fireflies = 0 until count map(_ => {
    Firefly.random(wall)
  })

  override def render(channel: MixerChannel): Unit = {
    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      localBuffer(x)(y) = 0
    }

    val bumps = fireflies collect {
      case f if f.step(wall) =>
        (f.x, f.y)
    }

    fireflies.foreach(f => {
      if (bumps.exists { case (x: Double, y: Double) => distance(x, y, f.x, f.y) < 5.0 }) {
        f.bump()
      }
      f.render(localBuffer)
    })

    for(x <- 0 until wall.pixelsX) {
      for(y <- 0 until wall.pixelsY) {
        channel.setPixel(x, y, localBuffer(x)(y))
      }
    }
  }
}

object Firefly {
  private val random = new Random(System.nanoTime())

  val clockLimit = 36

  def random(wall: Wall): Firefly = {
    new Firefly(random.nextDouble * (wall.pixelsX - 1), random.nextDouble * (wall.pixelsY - 1), random.nextDouble * 2 * Math.PI, random.nextDouble * clockLimit, clockLimit)
  }
}

class Firefly(var x: Double, var y: Double, var heading: Double, var clock: Double = 0.0, clockLimit: Int = 24, val speed: Double = 0.01, val clockStep: Double = 0.01) {

  val bumpSize = 15

  val flashLength = 0.3

  def step(wall: Wall): Boolean = {
    x = (x + Math.cos(heading) * speed + wall.pixelsX) % wall.pixelsX
    y = (y + Math.sin(heading) * speed + wall.pixelsY) % wall.pixelsY
    heading = (heading + Random.nextDouble / 100) % (2 * Math.PI)
    clock += clockStep
    clock >= clockLimit
  }

  def bump(): Unit = {
    if (clock < clockLimit) {
      clock += clockStep * bumpSize
    }
  }

  val darkColor: Int = Color.getHSBColor(0.56f, 0.95f, 0.1f).getRGB

  def render(buffer: Array[Array[Int]]): Unit = {
    if (clock >= clockLimit + 2 * flashLength) {
      clock = 0
    }
    if (clock < clockLimit) {
      buffer(x.toInt)(y.toInt) = darkColor
    } else {
      val brighness = if (clock < clockLimit + flashLength) {
        (clock - clockLimit) / flashLength
      } else {
        ((clockLimit + 2 * flashLength) - clock) / flashLength
      }
      val color = Color.getHSBColor(0.20f, 0.25f, brighness.toFloat).getRGB
      buffer(x.toInt)(y.toInt) = color
    }
  }
}