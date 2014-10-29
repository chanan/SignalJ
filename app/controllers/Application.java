package controllers;
import hubs.FirstTestPage;
import hubs.HelloWorld;
import play.mvc.Controller;
import play.mvc.Result;
import signalJ.GlobalHost;
import signalJ.services.HubContext;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render("Your new application is ready."));
    }
    
    public static Result test() {
    	return ok(views.html.test.render());
    }
    
    public static Result sendToHub() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    	HubContext<FirstTestPage> hub = GlobalHost.getHub(HelloWorld.class);
    	hub.clients().all.thisWillBeCalledFromOutsideTheHub("Hello From controller");
    	return ok();
    }

    public static Result signalr() {
        return ok(views.html.signalr.render());
    }
}