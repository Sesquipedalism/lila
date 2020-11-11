package lila.puzzle

import org.joda.time.DateTime

import lila.user.User

case class Round(
    id: Round.Id,
    date: DateTime,
    win: Boolean,
    vote: Option[Boolean],
    // tags: List[RoundTag],
    weight: Option[Int]
) {}

object Round {

  val idSep = ':'

  case class Id(userId: User.ID, puzzleId: Puzzle.Id) {

    override def toString = s"${userId}$idSep${puzzleId}"
  }

  object BSONFields {
    val id     = "_id"
    val date   = "d"
    val win    = "w"
    val vote   = "v"
    val weight = "w"
  }
}
