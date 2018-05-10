import java.awt.image._
import java.awt.{List => _, _}
import java.io.File
import javax.imageio.ImageIO
import javax.swing._

import akka.actor.{Actor, ActorSystem, Props}
import org.synthesis.Synthesis.getClass
import org.synthesis.webcam._

/**
  * Created by patrickbrown on 9/10/16.
  */
case class MainPanel(system: ActorSystem, fps: Int) extends JPanel with Runnable {

  val paletteB = new Array[Int](4096)
  var buffer = Array.ofDim[Int](1,1)
  var faceImages: List[BufferedImage] = List()
  var eyeImages: List[BufferedImage] = List()
  var iterationCount: Long = 0
  val fpses = scala.collection.mutable.ListBuffer[Double]()
  var pixel = new Array[Int](1)
  var widthL: Int = 0
  var heightL: Int = 0
  var imageBuffer: Image = null
  var mImageProducer: MemoryImageSource = null
  var imageUpdateB = false

  @Override
  override def imageUpdate(image: Image , a: Int, b: Int, c: Int, d: Int, e: Int): Boolean = {
    imageUpdateB
  }

  def getCompatibleColorModel(): ColorModel = {
    val gfx_config = GraphicsEnvironment.
      getLocalGraphicsEnvironment.getDefaultScreenDevice.
      getDefaultConfiguration
    gfx_config.getColorModel
  }

  def render() {
    val paletteShift = ((System.currentTimeMillis() % 100000) / 30.0).toInt

    val p = pixel

    if (p.length != widthL * heightL) return

    for(x <- 0 until widthL) {
      for(y <- 0 until heightL) {
        p(x + y * widthL) = paletteB((buffer(x)(y) + paletteShift) % 4096)
      }
    }
  }

  protected override def paintComponent(g: Graphics) {
    val t0 = System.nanoTime()
    super.paintComponent(g)
    iterationCount += 1

    render()

    if (mImageProducer != null) {
      mImageProducer.newPixels()

      g.drawImage(imageBuffer, 0, 0, this)

      faceImages foreach { f =>
        g.drawImage(f, widthL / 2 - f.getWidth / 2, heightL / 2 - f.getHeight / 2, this)
      }

      eyeImages foreach { f =>
        g.drawImage(f, widthL / 4 - f.getWidth / 2, heightL / 4 - f.getHeight / 2, this)
        g.drawImage(f, widthL * 3 / 4 - f.getWidth / 2, heightL / 4 - f.getHeight / 2, this)
      }
    }

    fpses += (1000000000 / (System.nanoTime() - t0))
    if (iterationCount % 10 == 0) {
      System.out.print(s"fps ${fpses.sum / fpses.size} \r")
      fpses.clear()
    }
  }

  class FaceListener extends Actor {

    context.system.eventStream.subscribe(self, classOf[Detections])

    def receive = {
      case FaceImages(images, _) =>
        faceImages = images
      case EyeImages(images, _) =>
        eyeImages = images
    }
  }

  override def run() {
    buffer = Array.ofDim[Int](getWidth,getHeight)

    val cm = getCompatibleColorModel()
    println(cm)
    widthL = getWidth
    heightL = getHeight
    val screenSize: Int = widthL * heightL
    if (pixel == null || pixel.length < screenSize) {
      pixel = new Array[Int](screenSize)
    }
    mImageProducer = new MemoryImageSource(widthL, heightL, cm, pixel, 0, widthL)
    mImageProducer.setAnimated(true)
    mImageProducer.setFullBufferUpdates(true)
    imageBuffer = Toolkit.getDefaultToolkit.createImage(mImageProducer)

    for(x <- 0 until 4096) {
      val red =  128.0 + (17.0*Math.sin(Math.PI * x / 139.0))
      val green = 128.0 + (113.0*Math.sin(Math.PI * x / 311.0))
      val blue = 128.0 + (97.0*Math.sin(Math.PI * x / 631.0))
      paletteB(x) = new Color(		red.toInt,
        green.toInt,
        blue.toInt).getRGB
    }

    for(x <- 0 until widthL) {
      for(y <- 0 until heightL) {
        buffer(x)(y) = ((
          128.0 + (128.0 * Math.sin(x / 64.0))
            + 128.0 + (128.0 * Math.sin(y / 32.0))
            + 128.0 + (128.0 * Math.sin((x + y) / 64.0))
            + 128.0 + (128.0 * Math.sin(Math.sqrt(x * x + y * y) / 32.0))
          ) / 4).toInt
      }
    }
    imageUpdateB = true
    val listener = system.actorOf(Props(new FaceListener()))

    while (true) {
      this.repaint()
      try {
        Thread.sleep(1000 / fps)
      }
      catch {
        case ex: InterruptedException => {
        }
      }
    }
  }
}