package controller;

import org.ninjax.core.Context;
import org.ninjax.core.Result;
import org.ninjax.core.properties.NinjaProperties;
import services.BasicService;
import views.LandingPage;


public class BasicController {
    
    public final BasicService basicService;
    public final NinjaProperties ninjaProperties;

    public BasicController(
            BasicService basicService,
            NinjaProperties ninjaProperties) {
        this.basicService = basicService;
        this.ninjaProperties = ninjaProperties;
    } 
    
    public Result helloWorld(Context context) {
        String appName = ninjaProperties.getOrDie("appname");
        
        String userName = context.getPathParameterEncoded("user").orElse("default");
        String landingPageHtml = LandingPage.render("Hello World " + userName + " running on " + appName).toString();
        
        return Result.ok().html(landingPageHtml);
    }
    
    public Result personJson(Context context) {
        var person = new Person("a name", 12);
        return Result.ok().json(person);
    }
    
    
    public Result parsePerson(Context context) {
        var personOpt = context.<Person>getJsonBody();
        
        if (personOpt.isPresent()) {
            return Result.ok().json(personOpt.get());
        } else {
            return Result.badRequest();
        }
    }

    
    public static record Person(String name, int age) {}
    
}
