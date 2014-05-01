package signalJ.services;

import akka.actor.UntypedActor;
import play.Logger;

/**
 * Created by Chanan on 4/30/2014.
 */
public class BlankActor extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Exception {
        Logger.debug("Blank!");
    }
}
