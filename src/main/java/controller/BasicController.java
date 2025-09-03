package controller;

import com.google.common.collect.ImmutableMap;
import org.ninjax.core.Request;
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
    
    public Result sessionTest(Request request) {
        var ninjaSession = request.getNinjaSession().orElseThrow();
        var time = ninjaSession.get("my-custome-time");
        
        var newNinjaSession = ninjaSession.withValue("my-custome-time", System.currentTimeMillis() + "");
        
        return Result
                .ok()
                .withNinjaSession(newNinjaSession)
                .text("ok - time in session is " + time);
    }
 
    public Result helloWorld(Request request) {
        String appName = ninjaProperties.get("appname").orElseThrow();
        
        String userName = request.getParameter("user").orElse("default");
        String landingPageHtml = LandingPage.render("Hello World " + userName + " running on " + appName).toString();
        
        return Result.ok().addHeader("testheader", "testvalue").html(landingPageHtml);
    }
    
    public Result personJson(Request request) {
        var person = new Person("a name", 12);
        return Result.ok().json(person);
    }
    
    
    public Result parsePerson(Request request) {
        var personOpt = request.<Person>getJsonBody();
        
        if (personOpt.isPresent()) {
            return Result.ok().json(personOpt.get());
        } else {
            return Result.badRequest();
        }
    }

    
    public static record Person(String name, int age) {}
    
}
