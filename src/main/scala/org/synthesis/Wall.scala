package org.synthesis

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.synthesis.controller.DesignAndController
import org.synthesis.design._
import org.synthesis.design.support.{Reset, SetPalette}
import org.synthesis.palette.{EmptyPaletteSet, PaletteSet}
import org.synthesis.skeleton.BodyList

import scala.collection.mutable
import scala.util.{Random, Try}

/**
  * Created by patrickbrown on 3/12/17.
  */
trait Wall extends Runnable {
  def pixelsX: Int
  def pixelsY: Int
  def fps: Int
  def system: ActorSystem
  def brightness: Double

  var bodyCount: Int = 0
  var lastBody: Long = System.currentTimeMillis()

  val bodyTime = 5000L // Millis

  val mixer = Mixer(pixelsX, pixelsY, 2, 1)

  val random = new Random(System.nanoTime())

  private var designs: Seq[DesignAndController] = Nil
  private var palettes: PaletteSet = EmptyPaletteSet
  private var permuations: Iterator[Seq[DesignAndController]] = Iterator.empty

  private var designSeqs: Seq[Seq[DesignAndController]] = Nil

  val ch1: MixerChannel = mixer.getChannel(0)
  val ch2: MixerChannel = mixer.getChannel(1)
  val channels = Seq(ch1, ch2)

  val alphaChannel: MixerChannel = mixer.getChannel(2)

  def render(): Unit

  def setDesigns(d: Seq[DesignAndController]): Boolean = {
    if (d == null) {
      false
    } else {
      designs = d
      State.initIfNeeded()
      true
    }
  }

  def setDesignSeqs(d: Seq[Seq[DesignAndController]]): Boolean = {
    if (d == null) {
      false
    } else {
      designSeqs = d
      true
    }
  }

  def setPalettes(p: PaletteSet): Boolean = {
    if (p == null) {
      false
    } else {
      palettes = p
      true
    }
  }

  def getBasePermutation: Seq[DesignAndController] = {
    Random.shuffle(designs)
  }

  def getNextPermutation: Seq[DesignAndController] = {
    if (designs.length > 1 && designSeqs.nonEmpty) {
      val (a, b) = getBasePermutation.splitAt(Random.nextInt(designs.length))

      a ++ designSeqs(Random.nextInt(designSeqs.length)) ++ b
    } else {
      getBasePermutation
    }
  }

  class BodyListener extends Actor {
    context.system.eventStream.subscribe(self, classOf[BodyList])

    def receive = {
      case BodyList(bs) =>
        bodyCount = bs.length
        if (bodyCount > 0) {
          lastBody = System.currentTimeMillis()
        }
    }
  }

  system.actorOf(Props(new BodyListener()))

  object State {
    var channelIndex = 0
    var crossfader = 0.0
    var currentDesign: Option[DesignAndController] = None
    var nextDesign: Option[DesignAndController] = None
    var direction = true
    var stableMillis: Long = 0

    var currentChannel: Option[MixerChannel] = None
    var nextChannel: Option[MixerChannel] = None

    currentChannel = Some(channels(channelIndex))
    nextChannel = Some(channels((channelIndex + 1) % channels.length))

    var modeStart = System.currentTimeMillis()

    val permutation: mutable.Queue[DesignAndController] = mutable.Queue()

    def updatePermutationIfNeeded(): Unit = {
      if (permutation.isEmpty) {
        permutation.enqueue(getNextPermutation:_*)
        if (currentDesign.isEmpty) {
          currentDesign = Try { permutation.dequeue() }.toOption
        }
        if (nextDesign.isEmpty) {
          nextDesign = Try { permutation.dequeue() }.toOption
        }
      }
    }

    def initIfNeeded(): Unit = {
      if (permutation.isEmpty) {
        permutation.enqueue(getBasePermutation:_*)
      }
      if (currentDesign.isEmpty) {
        currentDesign = getNextDesign
      }
      if (nextDesign.isEmpty) {
        nextDesign = getNextDesign
      }
    }

    def step(millis: Long): Boolean = {
      if (crossfader >= 1) {
        stableMillis += millis
      } else {
        crossfader += nextDesign.map(_.controller.fadeRatio * millis).getOrElse(0.001)
      }

      if (crossfader > 1) {
        crossfader = 1
      }

      crossfader >= 1 && milliTransition
    }

    private def milliTransition: Boolean = {
      stableMillis >= nextDesign.map(_.controller.stableMillis).getOrElse(1000L) &&
        (!nextDesign.exists(_.controller.bodyExt) || (System.currentTimeMillis() - lastBody > bodyTime) || stableMillis >= nextDesign.map(_.controller.maxMillis).getOrElse(0L))
    }

    private def getNextDesign: Option[DesignAndController] = {
      if (permutation.isEmpty) {
        permutation.enqueue(getNextPermutation: _*)
      }
      Try { permutation.dequeue() }.toOption
    }

    def transition(): Unit = {
      modeStart = System.currentTimeMillis()
      crossfader = 0
      stableMillis = 0

      channelIndex = (channelIndex + 1) % channels.length

      if (false && nextDesign.exists(_.controller.continue) && !currentDesign.exists(_.controller.continue)) {
        val temp = currentDesign
        currentDesign = nextDesign
        nextDesign = temp

        val tempC = currentChannel
        currentChannel = nextChannel
        nextChannel = tempC
      } else {
        if (nextDesign.exists(dc => dc.controller.continue)) {
          nextChannel.foreach { ch =>
            mixer.setRatio(ch, 0)
          }
        }

        currentDesign = nextDesign
        nextDesign = getNextDesign

        currentChannel = Some(channels(channelIndex))
        nextChannel = if (nextDesign.exists(dc => dc.controller.continue)) {
          Some(alphaChannel)
        } else {
          Some(channels((channelIndex + 1) % channels.length))
        }

        nextDesign.foreach { dac =>
          dac.design ! Reset
          palettes.getRandom.foreach { palette =>
            dac.design ! SetPalette(palette)
          }
        }
      }
    }
  }

  override def run() {
    mixer.setRatio(ch1, 1.0)

    var lastStep = System.currentTimeMillis()
    var itCount: Long = 0

    while (true) {
      val stepDiff = System.currentTimeMillis() - lastStep
      lastStep = System.currentTimeMillis()

      this.render()

      State.currentChannel.foreach { ch =>
        State.currentDesign.foreach(d => d.design ! Render(ch))
        if (!State.nextDesign.exists(_.controller.continue)) {
          mixer.setRatio(ch, 1.0 - State.crossfader)
        }
      }

      State.nextChannel.foreach { ch =>
        State.nextDesign.foreach(d => d.design ! Render(ch))
        if (!State.currentDesign.exists(_.controller.continue)) {
          mixer.setRatio(ch, State.crossfader)
        }
      }

      if (State.step(stepDiff)) {
        State.transition()
      }

      if (itCount % 120 == 0) {
        println(
          s"""

======== Wall =======

Current Design:   ${State.currentDesign.map(_.name).getOrElse("none")}
Next Design:      ${State.nextDesign.map(_.name).getOrElse("none")}
Crossfader:       ${State.crossfader}
Stable Millis:    ${State.stableMillis}
bodyCount:        $bodyCount
lastBody:         ${System.currentTimeMillis() - lastBody}
           """.stripMargin)
      }

      itCount += 1

      try {
        Thread.sleep(1000 / fps)
      } catch {
        case ex: InterruptedException =>
          println("Wall Interrupted")
      }
    }
  }
}
