package org.synthesis.design

import org.synthesis.design.support.Reset
import org.synthesis.{Mixer, MixerChannel, Wall}

import scala.util.Random

case class Static(wall: Wall) extends Design {
  private val canvas = Array.ofDim[Int](wall.pixelsX,wall.pixelsY)

  private var statics: List[StaticInt] = Nil

  val renderChance = 0.05
  val addChance = 0.05
  val growChance = 0.05

  class StaticInt(y: Int, x: Int, phaseA: Double, phaseB: Double, var size: Int) {

    def render(step: Double): Unit = {
      if (renderChance < Random.nextDouble) {
        val cs = currentSize(step)

        ((x - cs / 2).toInt until (x + cs / 2).toInt).foreach { xActual =>
          canvas(Math.floorMod(xActual, wall.pixelsX))(y) = Mixer.HSBtoRGB((Random.nextFloat(), Random.nextFloat(), 1.0f))
        }
      }
    }

    private def currentSize(step: Double): Double = {
      size * (Math.sin(step + phaseA) + Math.cos(step + phaseB))
    }

    def grow(): Unit = {
      size = size + 1
    }
  }

  reset()

  def reset(): Unit = {
    statics = Nil

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      canvas(x)(y) = 0
    }
  }

  override def support: Receive = {
    case Reset =>
      reset()
  }

  override def render(channel: MixerChannel): Unit = {
    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      canvas(x)(y) = 0
    }

    val step = (System.currentTimeMillis() % 100000).toDouble / 1000

    if (Random.nextDouble < addChance) {
      val temp = new StaticInt(Random.nextInt(wall.pixelsY), Random.nextInt(wall.pixelsX), Random.nextDouble * Math.PI * 2, Random.nextDouble * Math.PI * 2, Random.nextInt(5))
      statics = statics :+ temp
    }

    statics.foreach { s =>
      if (Random.nextDouble < growChance) {
        s.grow()
      }
    }

    statics.foreach(_.render(step))

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, canvas(x)(y))
    }
  }
}
