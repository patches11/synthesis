package org.synthesis.serial

import java.io.{InputStream, OutputStream}

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.io.IO
import akka.serial.Serial.Event
import akka.serial.{Serial, SerialSettings}
import akka.util.ByteString
import com.fazecast.jSerialComm.SerialPort
import org.synthesis.AfterTheLastMessage
import org.synthesis.webcam.WebcamFrame


/**
  * Created by Brown on 6/25/17.
  */
case class SerialPanel(serialPort: String, universeSize: Int, baud: Int = 230400) extends Actor with ActorLogging {
  private val state = Array.ofDim[Byte](universeSize)

  private val comPort: SerialPort = SerialPort.getCommPort(serialPort)

  private var output: OutputStream = null
  private var input: InputStream = null

  var iterationCount: Long = 0
  private val fpses = scala.collection.mutable.ListBuffer[Double]()

  object reader extends Runnable {
    override def run(): Unit = {
      while (true) {
        val readBuffer = new Array[Byte](1024)
        val numRead = comPort.readBytes(readBuffer, readBuffer.length)
        if (numRead > 0 && iterationCount % 120 == 0) {
          log.info(s"$serialPort read: '${formatA(ByteString.fromArray(readBuffer))}'")
        }
      }
    }
  }

  new Thread(reader).start()

  def setPixel(pixel: Int, color: Byte): Unit = {
    if (pixel < universeSize) {
      state(pixel) = color
    }
  }

  def setPixels(pixels: Array[Byte]): Unit = {
    pixels.copyToArray(state)
  }

  def update(): Unit = {
    val t0 = System.nanoTime()
    iterationCount += 1

    val msg = new VideoDisplayMessageBuilder(universeSize)
    msg.setMany(state)
    val built = msg.build()
    built.grouped(1024).foreach(ba => {
      output.write(ba)
    })

    fpses += (1000000000 / (System.nanoTime() - t0))
    if (iterationCount % 120 == 0) {
      System.out.print(f"SerialPanel $serialPort Update fps ${"%.1f".format(fpses.sum / fpses.size)} \r")
      fpses.clear()
    }
  }

  def setBrightness(brightness: Byte): Unit = {
    val message = new Array[Byte](2)
    message(0) = '#'.toByte
    message(1) = brightness

    output.write(message)
  }

  override def preStart() = {
    log.info(s"Requesting to open port: $serialPort")
    comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0)
    comPort.openPort

    output = comPort.getOutputStream
    input = comPort.getInputStream
    log.info(s"opened: $serialPort")
  }


  def receive = {
      case SetPixel(pixel, color) =>
        this.setPixel(pixel, color)

      case SetPixels(pixels) =>
        this.setPixels(pixels)

      case SetBrightness(brightness) =>
        this.setBrightness(brightness)

      case Update =>
        this.update()

      case Ack(msg) =>
        log.info(msg)
  }

  private def formatA(data: ByteString) = new String(data.toArray, "UTF-8")
}

case class Ack(msg: String) extends Event

case class SetPixel(pixel: Int, color: Byte)

case class SetPixels(pixels: Array[Byte])

case class SetBrightness(brightness: Byte)

case object Update


class VideoDisplayMessageBuilder(val universeSize: Int) extends SerialMessageBuilder {
  val dataSize: Int = (universeSize) + 1
  private val state = Array.ofDim[Byte](universeSize)

  def set(u: Int, v: Byte): Unit = {
    state(u) = v
  }

  def setMany(values: Map[Int, Byte]): Unit = {
    values.foreach { case ((u, v)) => state(u) = v }
  }

  def setMany(values: Array[Byte]): Unit = {
    values.copyToArray(state)
  }

  def build(): Array[Byte] = {
    val message = new Array[Byte](universeSize + 1)
    message(0) = '*'.toByte

    0 until universeSize foreach {i: Int =>
      message(1 + i) = state(i)
    }

    message
  }
}