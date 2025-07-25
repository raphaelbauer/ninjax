package core;

import controller.BasicController;
import org.ninjax.core.Router;

public class Routes {
    
    public Routes(
            Router router,
            BasicController basicController) {

        router.GET("/path/{test}").with(basicController::helloWorld);
        router.GET("/anotherpath").with(basicController::helloWorld);
        
    }
    
}
