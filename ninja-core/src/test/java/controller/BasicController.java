package controller;

import org.ninja.core.Request;
import org.ninja.core.Result;
import org.ninja.core.properties.NinjaProperties;
import services.BasicService;

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
                .builder()
                .ok()
                .withNinjaSession(newNinjaSession)
                .text("ok - time in session is " + time)
                .build();
    }
 
    public Result helloWorld(Request request) {
        String appName = ninjaProperties.get("appname").orElseThrow();
        
        String userName = request.getParameter("user").stream().findFirst().orElse("default");
        String landingPageHtml = """
                                 <html>
                                 <body>hello!</body>
                                 </html>
                                 """;
        
        return Result.builder().ok().addHeader("testheader", "testvalue").html(landingPageHtml).build();
    }
    
    public Result personJson(Request request) {
        var person = new Person("a name", 12);
        return Result.builder().ok().json(person).build();
    }
    
    
    public Result parsePerson(Request request) {
        var personOpt = request.<Person>getJsonBody();
        
        if (personOpt.isPresent()) {
            return Result.builder().ok().json(personOpt.get()).build();
        } else {
            return Result.builder().badRequest().build();
        }
    }

    
    public static record Person(String name, int age) {}
    
}
