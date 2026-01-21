package views;

import java.util.List;
import models.Guestbook;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;


public class GuestbookPage {
    
    public static NinjaHtmlTemplate render(List<Guestbook> guestbookEntries) {
        
        LayoutHtml layoutHtml = new LayoutHtml();
        
        NinjaHtmlTemplate juckula2Template = new NinjaHtmlTemplate();
        
        juckula2Template.html("""
                   <header class="jumbotron subhead">
                       <h1>Hi. This is a simple guestbook.</h1>
                       <p class="lead">I exemplify the usage
                       of the JDBI Ninja module.</p>
                   </header>
                   
                   <hr>
                   
                       <h2>Make a new guestbook entry:</h2>
                   
                       <form method="post" action="/post">
                           Email:   <input type="text" name="email" /><br/> 
                           Content: <input type="text" name="content" /><br/> 
                                   <input type="submit" value="submit">
                       </form>
                              """);
        
        
        juckula2Template.html("""
                              <h2>All previous entries:</h2>
                              """);
        
        
        
        for (var guestbook : guestbookEntries) {
                    juckula2Template.html("""
                            <h3>Entry</h3>
                            <p>
                                Email: """, guestbook.email, 
                                """
                                <br/>
                                Content: """, guestbook.content,
                                """
                            </p>
                              """);
        }
        
        
        return layoutHtml.render("Guestbook Entries", juckula2Template);   
    }

}
