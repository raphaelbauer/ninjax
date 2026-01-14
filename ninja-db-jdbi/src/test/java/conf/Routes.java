package conf;

import controller.GuestbookController;
import org.ninja.core.AssetsController;
import org.ninja.core.Router;

public class Routes {
    
    public Routes(
            Router router,
            GuestbookController guestbookController,
            AssetsController assetsController) {
        
        router.GET("/").with(guestbookController::index);
        router.POST("/post").with(guestbookController::post);
        
        router.GET("/files/{fileName}").with(assetsController::serveStatic);
    }
    
}
