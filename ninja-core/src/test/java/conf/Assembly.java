package conf;

import controller.BasicController;
import org.ninja.core.AssetsController;
import org.ninja.core.NinjaJetty;
import org.ninja.core.Router;
import org.ninja.core.properties.NinjaProperties;
import services.BasicService;


public class Assembly {
    
    public final NinjaProperties ninjaProperties = new NinjaProperties();

    public final MyCustomFilter myCustomFilter = new MyCustomFilter();
    
    public final BasicService basicService = new BasicService();
    public final BasicController basicController = new BasicController(basicService, ninjaProperties);
     
    public final AssetsController assetsController = new AssetsController();
    
    public final Router router = new Router();
    public final Routes routes = new Routes(router, basicController, assetsController, myCustomFilter);
    
    public final NinjaJetty ninja = new NinjaJetty(router, ninjaProperties);
    

    public static void main(String [] a) {
        new Assembly();
    }

}
