package org.synthesis.design

import akka.actor.{Actor, Props}
import org.synthesis.palette.Palette
import org.synthesis.skeleton.BodyList
import org.synthesis.util.Activate
import org.synthesis.{Mixer, MixerChannel, Wall}

import scala.collection.mutable

case class Saskia160518(wall: Wall, defaultPalette: Palette)  extends Design  {
  import org.synthesis.Utils._

  val speed = 60.toDouble / 100000
  val maxCircleSize = 1.0
  val numStrands = 2
  val numColumns = Math.ceil(wall.pixelsX.toDouble / 2).toInt
  val numRows = wall.pixelsY / 5

  private val canvas = Array.ofDim[Int](wall.pixelsX, wall.pixelsY)

  private val colorA = Mixer.getRGB(255, 253, 174, 120)
  private val colorB = Mixer.getRGB(255,226, 129, 161)

  private val paletteMod = 40

  private val palette = true
  private val body = true
  private val locMax = 640
  private var bodyLocs: List[Int] = List()

  for (x <- 0 until wall.pixelsX;
       y <- 0 until wall.pixelsY) yield {
    canvas(x)(y) = 0
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

    var lastBody = System.currentTimeMillis()

    def receive = waiting

    def painting: Receive = {
      case Activate =>
        lastActivate = System.currentTimeMillis()
      case BodyList(bodies) =>
        if (bodies.nonEmpty || System.currentTimeMillis() - lastBody > 2 * 1000) {
          bodyLocs = bodies.map(_.center.x)
          lastBody = System.currentTimeMillis()
        }
    }
  }

  val bodyListener = context.system.actorOf(Props(new BodyListener()))

  def getColor(strandPhase: Double, row: Double): Int = {
    if (palette) {
      if (body && bodyLocs.nonEmpty) {
        val colorPosition = bodyLocs.sum.toDouble / bodyLocs.length / 640 * defaultPalette.length
        defaultPalette.getColor(colorPosition.toInt)
      } else {
        defaultPalette.getColor((strandPhase + row.toDouble / numRows).toInt * paletteMod)
      }
    } else {
      Mixer.blend(colorA, colorB, row.toDouble / numRows)
    }
  }

  override def render(channel: MixerChannel): Unit = {
    val phase = System.currentTimeMillis() * speed

    if (iterationCount % 10 == 0) {
      bodyListener ! Activate
    }

    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      canvas(x)(y) = 0
    }

    numStrands.times { strand: Int =>
      val strandPhase = phase + strand.toDouble / numStrands * 2 * Math.PI

      numColumns.times { col: Int =>
        val colOffset = col.toDouble / numColumns * 2 * Math.PI
        val x = col * 2

        numRows.times { row: Int =>
          val y: Double = wall.pixelsY / 2 - numRows / 2 + row + Math.sin(strandPhase + colOffset) * wall.pixelsY / 4
          val sizeOffset = (Math.cos(strandPhase - (row.toDouble / numRows) + colOffset) + 1) * 0.5
          val circleSize = sizeOffset * maxCircleSize

          val color = getColor(strandPhase, row)
          val (h, s, b) = Mixer.RGBtoHSB(color)

          canvas(x)(y.toInt) = Mixer.HSBtoRGB((h, s , b * circleSize.toFloat))
        }
      }
    }



    for (x <- 0 until wall.pixelsX;
         y <- 0 until wall.pixelsY) yield {
      channel.setPixel(x, y, canvas(x)(y))
    }
  }
}
