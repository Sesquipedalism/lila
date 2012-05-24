package controllers

import lila._
import views._
import security.Permission
import user.GameFilter
import http.Context

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

object User extends LilaController {

  def userRepo = env.user.userRepo
  def paginator = env.user.paginator
  def gamePaginator = env.game.paginator
  def forms = user.DataForm

  def show(username: String) = showFilter(username, "all", 1)

  def showFilter(username: String, filterName: String, page: Int) = Open { implicit ctx ⇒
    IOptionIOk(userRepo byUsername username) { u ⇒
      u.enabled.fold(
        env.user.userInfo(u, ctx) map { info ⇒
          val filters = user.GameFilterMenu(info, ctx.me)
          val filter = filters(filterName)
          html.user.show(u, info,
            games = gamePaginator(filterPaginator(u, filter))(page),
            filters = filters,
            filter = filter)
        },
        io(html.user.disabled(u)))
    }
  }

  def filterPaginator(user: User, filter: GameFilter)(implicit ctx: Context) =
    filter match {
      case GameFilter.All ⇒ gamePaginator.userAdapter(user)
      case GameFilter.Me  ⇒ gamePaginator.opponentsAdapter(user, ctx.me | user)
    }

  def list(page: Int) = Open { implicit ctx ⇒
    IOk(onlineUsers map { html.user.list(paginator elo page, _) })
  }

  val online = Open { implicit ctx ⇒
    IOk(onlineUsers map { html.user.online(_) })
  }

  val autocomplete = Action { implicit req ⇒
    get("term", req).filter(""!=).fold(
      term ⇒ JsonOk((userRepo usernamesLike term).unsafePerformIO),
      BadRequest("No search term provided")
    )
  }

  val getBio = Auth { ctx ⇒ me ⇒ Ok(me.bio) }

  val setBio = AuthBody { ctx ⇒
    me ⇒
      implicit val req = ctx.body
      IORedirect(forms.bio.bindFromRequest.fold(
        f ⇒ putStrLn(f.errors.toString) map { _ ⇒ routes.User show me.username },
        bio ⇒ userRepo.setBio(me, bio) map { _ ⇒ routes.User show me.username }
      ))
  }

  val close = Auth { implicit ctx ⇒
    me ⇒
      Ok(html.user.close(me))
  }

  val closeConfirm = Auth { ctx ⇒
    me ⇒
      IORedirect {
        userRepo disable me map { _ ⇒ routes.User show me.username }
      }
  }

  def engine(username: String) = Secure(Permission.MarkEngine) { _ ⇒
    _ ⇒
      IORedirect {
        userRepo toggleEngine username map { _ ⇒ routes.User show username }
      }
  }

  def mute(username: String) = Secure(Permission.MutePlayer) { _ ⇒
    _ ⇒
      IORedirect {
        userRepo toggleMute username map { _ ⇒ routes.User show username }
      }
  }

  val signUp = TODO

  val stats = TODO

  def export(username: String) = TODO

  val onlineUsers: IO[List[User]] = userRepo byUsernames env.user.usernameMemo.keys
}
