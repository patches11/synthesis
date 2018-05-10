package org.synthesis

import java.awt.BorderLayout
import javax.swing.JFrame

import akka.actor.{ActorSystem, Props}
import org.opencv.core._
import org.synthesis.controller._
import org.synthesis.design._
import org.synthesis.kinect.{DepthPanel, KinectMaster, SkeletonDetector}
import org.synthesis.palette.{AllPalettes, RainbowPalette}
import org.synthesis.palette.{AllPalettes, RainbowPalette, Red}
import org.synthesis.webcam.{FaceDetector, WebcamPanel}

/**
  * Created by patrickbrown on 9/10/16.
  */

object Synthesis extends App {
  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  import org.synthesis.controller.Implicits._

  val system = ActorSystem("SynthesisSystem")

//    new Thread(WebcamMaster(system)).start()
    val faceDetector = system.actorOf(Props(new FaceDetector()))
//  val fingerDetector = system.actorOf(Props(new FingerDetector()))

  /*
  val webcamFrame = new JFrame("Webcam")
  webcamFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

  val webcamPanel = WebcamPanel(system, 60)

  webcamFrame.setLayout(new BorderLayout())
  webcamFrame.add(webcamPanel, BorderLayout.CENTER)
  webcamFrame.setSize(720, 405)
  webcamFrame.setVisible(true)
  webcamFrame.setLocationRelativeTo(null)

  new Thread(webcamPanel).start()*/


/*
  val jframe = new JFrame("Synthesis")
  jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)


  com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(jframe, true)

  val output = WallSimulationPanel(system, 60, 50, 50, 0.5)

  jframe.setLayout(new BorderLayout())
  jframe.add(output, BorderLayout.CENTER)
  jframe.setSize(800, 800)
  jframe.setVisible(true)
  jframe.setLocationRelativeTo(null)
*/
  // Teensys:
  // /dev/cu.usbmodem3175971
  // /dev/cu.usbmodem3174201

  // /dev/cu.usbmodem3175951
  // /dev/cu.usbmodem3373831


  val kinect = new KinectMaster(system)

  val skelDetector = system.actorOf(Props(new SkeletonDetector()))

  //val bodyListener = system.actorOf(Props(new BodyListener()))
/*
  val depthFrame = new JFrame("Depth")
  depthFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  depthFrame.setLocation(0, 0)

  val depthPanel = new DepthPanel(system, 60)

  depthFrame.setLayout(new BorderLayout())
  depthFrame.add(depthPanel, BorderLayout.CENTER)
  depthFrame.setSize(640, 480)
  depthFrame.setVisible(true)
  depthFrame.setLocationRelativeTo(null)

    new Thread(depthPanel).start()
*/


  val output = WallSerial2p5(system = system,
    fps = 60,
    pixelsX = 50,
    pixelsY = 50,
    brightness = 0.5,
    panelsX = 2,
    panelsY = 2,
    gammaCorrection = false,
    //devices = Seq(("/dev/cu.usbmodem3373831", false),("/dev/cu.usbmodem3174201", true),  ("/dev/cu.usbmodem3175951", false), ("/dev/cu.usbmodem3175971", true)))
    //devices = Seq(("/dev/cu.usbmodem3373831", false),("/dev/cu.usbmodem3174201", true), ("/dev/cu.usbmodem3175971", false), ("/dev/cu.usbmodem3175951", true)))
    //devices = Seq(("/dev/cu.usbmodem3373831", false), ("/dev/cu.usbmodem3175971", true), ("/dev/cu.usbmodem3174201", false), ("/dev/cu.usbmodem3175951", true)))
	  //devices = Seq(("/dev/ttyACM0", false)))
	  devices = Seq(("/dev/ttyACM2", false), ("/dev/ttyACM3", true),("/dev/ttyACM1", false),  ("/dev/ttyACM0", true)))

  val designsA: Seq[DesignAndController] = Seq(
    system.actorOf(Props(RainbowCircle(output, RainbowPalette))),
    DesignAndController(system.actorOf(Props(Painting(output))), PaintingController),
    DesignAndController(system.actorOf(Props(SimpleFace(output))), DefaultFaceController)
    //system.actorOf(Props(WipeSideways(output))),
    //system.actorOf(Props(Plasma(output))),
    //system.actorOf(Props(Rainbow(output))),
    //system.actorOf(Props(SpaceFillingCurves(output))),
    //system.actorOf(Props(FireflyHerd(output)))
    //system.actorOf(Props(Pippa(output)))
    //system.actorOf(Props(TrippyTree(output))),
    //system.actorOf(Props(Forrest(output))),
    //system.actorOf(Props(Painting(output, system))),
//    DesignAndController(system.actorOf(Props(SimpleFace(output, system))), DefaultFaceController),
//    DesignAndController(system.actorOf(Props(Pippa(output))), DefaultFaceController)
  )

  val designsProd: Seq[DesignAndController] = Seq(
    DesignAndController(system.actorOf(Props(RainbowCircle(output, RainbowPalette))), name = "RainbowCircle"),
    DesignAndController(system.actorOf(Props(Rainbow(output, RainbowPalette))), name = "Rainbow"),
    DesignAndController(system.actorOf(Props(SpaceFillingCurves(output))), name = "SpaceFillingCurves"),
    DesignAndController(system.actorOf(Props(FireflyHerd(output))), name = "FireflyHerd"),
    DesignAndController(system.actorOf(Props(Fire(output))), name = "Fire"),
    DesignAndController(system.actorOf(Props(Saskia160518(output, RainbowPalette))), PaintingController, name = "Saskia160518"),
    DesignAndController(system.actorOf(Props(Painting(output))), PaintingController, name = "Painting"),
    DesignAndController(system.actorOf(Props(LoadedString(output, RainbowPalette))), PaintingController, name = "LoadedString"),
    DesignAndController(system.actorOf(Props(TrippyTree(output))), name = "TrippyTree")
  )

  val designSeqs: Seq[Seq[DesignAndController]] = Seq(
    Seq(
      DesignAndController(system.actorOf(Props(Static(output))), StaticController, "Static"),
      DesignAndController(system.actorOf(Props(Depth(output, Red))), DepthController, "Depth"),
      DesignAndController(system.actorOf(Props(Pippa(output))), PippaController, "Pippa")
    )
  )

  // ID
  val init: Seq[DesignAndController] = Seq(system.actorOf(Props(Test(output))))

  val test: Seq[DesignAndController] = Seq(system.actorOf(Props(LoadedString(output, RainbowPalette))))
  val depth: Seq[DesignAndController] = Seq(system.actorOf(Props(Depth(output, Red))))
  val fire: Seq[DesignAndController] = Seq(system.actorOf(Props(FireflyHerd(output))))
  val tt: Seq[DesignAndController] = Seq(system.actorOf(Props(TrippyTree(output))))
  val sk: Seq[DesignAndController] = Seq(system.actorOf(Props(Saskia160518(output, RainbowPalette))))
  val painting: Seq[DesignAndController] = Seq(DesignAndController(system.actorOf(Props(Painting(output))), PaintingController))
  val rainbow: Seq[DesignAndController] = Seq(
    system.actorOf(Props(RainbowCircle(output, RainbowPalette))),
    system.actorOf(Props(Rainbow(output, RainbowPalette)))
  )

  val t2: Seq[DesignAndController] = Seq(system.actorOf(Props(Rainbow(output, RainbowPalette))), DesignAndController(system.actorOf(Props(SimpleFace(output))), DefaultFaceController))
  val t3: Seq[DesignAndController] = Seq(system.actorOf(Props(Rainbow(output, RainbowPalette))), DesignAndController(system.actorOf(Props(Static(output))), StaticController))

  output.setPalettes(AllPalettes)
  output.setDesigns(designsProd)
  output.setDesignSeqs(designSeqs)

  new Thread(output).start()

}


