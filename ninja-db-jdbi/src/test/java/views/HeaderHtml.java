package views;

import org.ninja.htmltemplate.NinjaHtmlTemplate;

public class HeaderHtml {

    public static NinjaHtmlTemplate render() {
        NinjaHtmlTemplate juckula2Template = new NinjaHtmlTemplate();

        juckula2Template.html(
                """
                <header>
                <p>That's the funky header, duderino!</p>
                </header>
                """
        );
        
        return juckula2Template;
    }

}
