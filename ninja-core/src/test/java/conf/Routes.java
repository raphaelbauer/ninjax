package conf;

import controller.BasicController;
import org.ninja.core.AssetsController;
import org.ninja.core.Router;

public class Routes {
    
    public Routes(
            Router router,
            BasicController basicController,
            AssetsController assetsController,
            MyCustomFilter myCustomFilter) {

        router.GET("/hello/{user}").filter(myCustomFilter).with(basicController::helloWorld);
        router.GET("/anotherpath").with(basicController::helloWorld);
        
        router.GET("/session-test").with(basicController::sessionTest);
        
        router.GET("/person").with(basicController::personJson);
        router.POST("/person").with(basicController::parsePerson);
        
        router.GET("/files/{fileName}").with(assetsController::serveStatic);
    }
    
}
