package org.synthesis.skeleton

case class Body(
                 center: InterestPoint,
                 head: Option[InterestPoint] = None,
                 leftHand: Option[InterestPoint] = None,
                 rightHand: Option[InterestPoint] = None,
                 leftFoot: Option[InterestPoint] = None,
                 rightFoot: Option[InterestPoint] = None
               ) {
  def allPoints: List[InterestPoint] = {
    List(center) ++ head.toList ++ leftHand.toList ++ rightHand.toList ++ leftFoot.toList ++ rightFoot.toList
  }
}

object Body {
  def create(agex: List[(Int, Int, Int)], cx: Int, cy: Int, cDepth: Int, resample: Int): Option[Body] = {

    val (left, top, right, bottom) = agex.foldLeft((cx, cy, cx, cy)) { case ((l, t, r, b), (x, y, _)) =>
      val nl = if (x < l) x else l
      val nt = if (y < t) y else t
      val nr = if (x > r) x else r
      val nb = if (y > b) y else b
      (nl, nt, nr, nb)
    }

    val lrSpread = (right - left) * resample
    val tbSpread = (bottom - top) * resample

    if (lrSpread > 100 && tbSpread > 100) {
      val base = Body(InterestPoint.desample(cx, cy, resample, InterestPoint.Center, cDepth))

      Some(agex.foldLeft(base) { case (acc, (x, y, depth)) =>
        if (x == left) {
          acc.copy(rightHand = Some(InterestPoint.desample(x, y, resample, InterestPoint.RightHand, depth)))
        } else if (x == right) {
          acc.copy(leftHand = Some(InterestPoint.desample(x, y, resample, InterestPoint.LeftHand, depth)))
        } else if (y == top) {
          acc.copy(head = Some(InterestPoint.desample(x, y, resample, InterestPoint.Head, depth)))
        } else {
          acc
        }
      })
    } else {
      None
    }


  }

}
