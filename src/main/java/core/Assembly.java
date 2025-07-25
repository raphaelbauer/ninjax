package core;

import controller.BasicController;
import org.ninjax.core.NinjaJetty;
import org.ninjax.core.Router;
import services.BasicService;


public class Assembly {
    
    public final BasicService basicService = new BasicService();
    public final BasicController basicController = new BasicController(basicService);
    
    public final Router router = new Router();
    public final Routes routes = new Routes(router, basicController);
    
    public final NinjaJetty ninja = new NinjaJetty(router);
    
    public static void main(String [] a) {
        new Assembly();
    }

}
