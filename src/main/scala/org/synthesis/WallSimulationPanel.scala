package org.synthesis

import java.awt.event.{ComponentAdapter, ComponentEvent}
import java.awt.image.{ColorModel, MemoryImageSource}
import java.awt.{Graphics, GraphicsEnvironment, Image, Toolkit}
import javax.swing.JPanel

import scala.collection.mutable.ArrayBuffer
import akka.actor._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by patrickbrown on 3/12/17.
  */
case class WallSimulationPanel(system: ActorSystem, fps: Int, pixelsX: Int, pixelsY: Int, brightness: Double) extends JPanel with Wall {

  val paletteB = new Array[Int](4096)
  private val buffers = ArrayBuffer[Mixer]()
  var iterationCount: Long = 0
  private val fpses = scala.collection.mutable.ListBuffer[Double]()
  var pixel = new Array[Int](1)
  var widthL: Int = 0
  var heightL: Int = 0
  var imageBuffer: Image = _
  var mImageProducer: MemoryImageSource = _
  var offsetX: Int = 0
  var offsetY: Int = 0

  var ratio: Int = 0
  var borderSize: Int = 0

  resize()

  private def resize(): Unit = {
    val cm = getCompatibleColorModel
    println(cm)
    widthL = getWidth
    heightL = getHeight
    val screenSize: Int = widthL * heightL

    pixel = new Array[Int](screenSize)

    mImageProducer = new MemoryImageSource(widthL, heightL, cm, pixel, 0, widthL)
    mImageProducer.setAnimated(true)
    mImageProducer.setFullBufferUpdates(true)
    imageBuffer = Toolkit.getDefaultToolkit.createImage(mImageProducer)

    val xRatio = widthL / pixelsX
    val yRatio = heightL / pixelsY

    if (xRatio < yRatio) {
      ratio = xRatio
    } else {
      ratio = yRatio
    }

    borderSize = ratio / 5

    offsetX = (widthL - pixelsX * ratio) / 2 - borderSize / 2
    offsetY = (heightL - pixelsY * ratio) / 2 - borderSize / 2

    for(x <- 0 until widthL) {
      for (y <- 0 until heightL) {
        if (x < offsetX || x  >= widthL - offsetX || y < offsetY || y >= heightL - offsetY) {
          pixel(x + y * widthL) = 0
        } else {
          pixel(x + y * widthL) = Int.MaxValue
        }
      }
    }
  }

  class ResizeListener extends ComponentAdapter {
    override def componentResized(e: ComponentEvent): Unit = {
      resize()
    }
  }

  this.addComponentListener(new ResizeListener)

  private def getCompatibleColorModel: ColorModel = {
    val gfx_config = GraphicsEnvironment.
      getLocalGraphicsEnvironment.getDefaultScreenDevice.
      getDefaultConfiguration
    gfx_config.getColorModel
  }

  private def internalRender() {
    val p = pixel

    if (p.length != widthL * heightL) return

    for(x <- 0 until widthL) {
      for(y <- 0 until heightL) {
        val xMod = x / ratio
        val yMod = y / ratio
        if (xMod < pixelsX && yMod < pixelsY && x % ratio > borderSize - 1 && y % ratio > borderSize - 1) {
          val color = if (false) {
            Mixer.darken(mixer.outputBuffer(xMod)(yMod), 1 - brightness, 0.01)
          } else {
            mixer.outputBuffer(xMod)(yMod)
          }

          p((x + offsetX) + (y + offsetY) * widthL) = color
        }
      }
    }

  }

  case class RenderActor() extends Actor {
    override def receive: Receive = {
      case DoRender() =>
        internalRender()
    }
  }

  case class DoRender()

  protected override def paintComponent(g: Graphics) {
    val t0 = System.nanoTime()
    super.paintComponent(g)
    iterationCount += 1

    if (mImageProducer != null) {
      mImageProducer.newPixels()

      g.drawImage(imageBuffer, 0, 0, this)
    }

    fpses += (1000000000 / (System.nanoTime() - t0))
    if (iterationCount % 120 == 0) {
      System.out.print(f"Simulation Panel fps ${"%.1f".format(fpses.sum / fpses.size)} \r")
      fpses.clear()
    }
  }

  val renderer = system.actorOf(Props(RenderActor()))

  override def render(): Unit = {
    Future {
      this.repaint()

      renderer ! DoRender()
    }
  }
}
