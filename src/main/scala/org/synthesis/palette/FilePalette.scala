package org.synthesis.palette

import java.io.File
import javax.imageio.ImageIO

case class FilePalette(file: File) extends Palette {
  val name: String = file.getName
  private val image = ImageIO.read(file)

  override def length: Int = image.getWidth()

  override def getColor(index: Int): Int = image.getRGB(index % length, 0)
}
