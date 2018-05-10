package org.synthesis.skeleton

case class InterestPoint(x: Int, y: Int, kind: InterestPoint.Kind = InterestPoint.None, depth: Option[Int] = scala.None)

object InterestPoint {
  def desample(x: Int, y: Int, r: Int, kind: InterestPoint.Kind = InterestPoint.None, depth: Int): InterestPoint = {
    InterestPoint(x * r, y * r, kind, Some(depth))
  }

  sealed trait Kind
  case object None extends Kind
  case object Center extends Kind
  case object Head extends Kind
  case object LeftHand extends Kind
  case object RightHand extends Kind
  case object LeftFoot extends Kind
  case object RightFoot extends Kind
}
