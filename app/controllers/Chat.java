package controllers;

import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by Chanan on 4/18/2014.
 */
public class Chat extends Controller {
    public static Result index() {
        return ok(views.html.chatindex.render());
    }

    public static Result chatRoom(String username) {
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Chat.index());
        }
        return ok(views.html.chat.render(username));
    }
}