package views;

import java.util.List;
import java.util.Map;
import models.Guestbook;
import org.ninjax.htmltemplate.Html;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;
import org.ninjax.htmltemplate.NinjaHtmlTemplateTool;

public class GuestbookPage {

    public static NinjaHtmlTemplate render(List<Guestbook> guestbookEntries) {

        LayoutHtml layoutHtml = new LayoutHtml();

        NinjaHtmlTemplate ninjaHtmlTemplate = new NinjaHtmlTemplate();

        ninjaHtmlTemplate.append(
                """
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

        ninjaHtmlTemplate.append("""
                              <h2>All previous entries:</h2>
                              """);

        for (var guestbook : guestbookEntries) {

            String template
                    = """
                        <h3>Entry</h3>
                            <p>
                                Email: {{guestbookEmail}}
                                <br/>
                                Content: {{guestbookContent}}
                            </p>
                          """;

            String templateWithReplacedValues = NinjaHtmlTemplateTool.replacePlaceholders(
                    template, Map.of("guestbookEmail", guestbook.email, "guestbookContent", guestbook.content));

            ninjaHtmlTemplate.append(new Html(templateWithReplacedValues));

            
        }
        
        return layoutHtml.render("Guestbook Entries", ninjaHtmlTemplate);

    }
}
