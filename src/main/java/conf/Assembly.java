package conf;

import controller.BasicController;
import controller.GuestbookController;
import org.ninjax.core.AssetsController;
import org.ninjax.core.NinjaJetty;
import org.ninjax.core.Router;
import org.ninjax.core.properties.NinjaProperties;
import org.ninjax.db.flyway.NinjaFlywayMigrator;
import org.ninjax.db.hikari.NinjaDbHikariProvider;
import org.ninjax.db.jdbc.NinjaDatasourcePropertiesExtractor;
import org.ninjax.db.jdbi.NinjaJdbiImpl;
import services.BasicService;
import services.GuestbooksService;


public class Assembly {
    
    public final NinjaProperties ninjaProperties = new NinjaProperties();
    
    
    ////////////////////////////////////////////////////////////////////////////
    // DB Configuration
    final private NinjaDatasourcePropertiesExtractor ninjaDatasourceConfigProvider = new NinjaDatasourcePropertiesExtractor(ninjaProperties);
    
    final private NinjaFlywayMigrator ninjaFlywayMigrator = new NinjaFlywayMigrator(ninjaDatasourceConfigProvider.get());
    
    final private NinjaDbHikariProvider ninjaDbHikariProvider = new NinjaDbHikariProvider(ninjaDatasourceConfigProvider.get());
    final private NinjaJdbiImpl ninjaJdbiImpl = new NinjaJdbiImpl(ninjaDbHikariProvider.get());
    // end
    

    public final MyCustomFilter myCustomFilter = new MyCustomFilter();
    
    public final BasicService basicService = new BasicService();
    public final BasicController basicController = new BasicController(basicService, ninjaProperties);
    
    public final GuestbooksService guestbooksService = new GuestbooksService(ninjaJdbiImpl);
    public final GuestbookController guestbookController = new GuestbookController(guestbooksService);
     
    public final AssetsController assetsController = new AssetsController();
    
    public final Router router = new Router();
    public final Routes routes = new Routes(router, basicController, guestbookController, assetsController, myCustomFilter);
    
    public final NinjaJetty ninja = new NinjaJetty(router, ninjaProperties);
    

    public static void main(String [] a) {
        new Assembly();
    }

}
