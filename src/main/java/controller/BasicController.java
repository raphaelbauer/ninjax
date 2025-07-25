package controller;

import org.ninjax.core.Context;
import org.ninjax.core.Result;
import services.BasicService;
import views.LandingPage;


public class BasicController {
    
    public final BasicService basicService;

    public BasicController(BasicService basicService) {
        this.basicService = basicService;
    } 
    
    public Result helloWorld(Context context) {
        return Result.ok().html(LandingPage.render("Hello World " + context.getPathParam("test")).toString());
    }
    
}
