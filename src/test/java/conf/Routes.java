package conf;

import controller.BasicController;
import controller.GuestbookController;
import org.ninjax.core.AssetsController;
import org.ninjax.core.Router;

public class Routes {
    
    public Routes(
            Router router,
            BasicController basicController,
            GuestbookController guestbookController,
            AssetsController assetsController,
            MyCustomFilter myCustomFilter) {

        router.GET("/hello/{user}").filter(myCustomFilter).with(basicController::helloWorld);
        router.GET("/anotherpath").with(basicController::helloWorld);
        
        router.GET("/session-test").with(basicController::sessionTest);
        
        router.GET("/person").with(basicController::personJson);
        router.POST("/person").with(basicController::parsePerson);
        
        router.GET("/").with(guestbookController::index);
        router.POST("/post").with(guestbookController::post);
        
        router.GET("/files/{fileName}").with(assetsController::serveStatic);
    }
    
}
