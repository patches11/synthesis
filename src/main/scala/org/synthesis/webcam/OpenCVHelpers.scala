package org.synthesis.webcam


import java.awt.image.BufferedImage

import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
  * Created by patrickbrown on 9/24/16.
  */
object OpenCVHelpers {
  implicit class MatHelper(theMat: Mat) {
    def toBuffer: BufferedImage = {
      val newMat = new Mat()
      Imgproc.cvtColor(theMat, newMat, Imgproc.COLOR_RGB2BGRA, 0)

      val data = newMat.toArray

      val img = new BufferedImage(newMat.width(), newMat.height(), BufferedImage.TYPE_4BYTE_ABGR)
      img.getRaster.setDataElements(0, 0, newMat.width(), newMat.height(), data)
      img
    }

    def toArray: Array[Byte] = {
      val data = new Array[Byte]((theMat.width() * theMat.height() * theMat.elemSize()).toInt)
      theMat.get(0, 0, data)
      data
    }
  }
}
