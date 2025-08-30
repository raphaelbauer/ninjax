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
    
    public Result sessionTest(Context context) {
        var ninjaSession = context.getNinjaSession();
        var time = ninjaSession.get("my-custome-time");
        
        ninjaSession.put("my-custome-time", System.currentTimeMillis() + "");
        
        return Result.ok().text("ok - time in session is " + time);
    }
 
    public Result helloWorld(Context context) {
        String appName = ninjaProperties.get("appname").orElseThrow();
        
        String userName = context.getPathParameterEncoded("user").orElse("default");
        String landingPageHtml = LandingPage.render("Hello World " + userName + " running on " + appName).toString();
        
        return Result.ok().addHeader("testheader", "testvalue").html(landingPageHtml);
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
