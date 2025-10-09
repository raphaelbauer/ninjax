package controller;

import java.util.List;
import models.Guestbook;
import org.ninjax.core.Request;
import org.ninjax.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.GuestbooksService;
import views.GuestbookPage;


public class GuestbookController {

    private final static Logger logger = LoggerFactory.getLogger(GuestbookController.class);

    private final GuestbooksService guestbooksService;

    public GuestbookController(GuestbooksService guestbooksService) {
        this.guestbooksService = guestbooksService;
    }

    public Result index(Request request) {

        // Get all guestbookentries now:
        List<Guestbook> guestBookEntries = guestbooksService.listGuestBookEntries();
        // Default rendering is simple by convention
        // This renders the page in views/ApplicationController/index.ftl.html

        var guestbookPage = GuestbookPage.render(guestBookEntries);
        
        return Result.ok().html(guestbookPage.toString());
    }

    public Result post(Request request) { 
        String email = request.getParameter("email").stream().findFirst().orElseThrow();
        String content = request.getParameter("content").stream().findFirst().orElseThrow();
        
        Guestbook guestbook = new Guestbook(email, content);
        
        guestbooksService.createGuestbook(guestbook);
        return Result.redirect("/");
    }

}
