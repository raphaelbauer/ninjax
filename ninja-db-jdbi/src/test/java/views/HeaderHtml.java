package views;

import org.ninjax.htmltemplate.NinjaHtmlTemplate;

public class HeaderHtml {

    public static NinjaHtmlTemplate render() {
        NinjaHtmlTemplate ninjaHtmlTemplate = new NinjaHtmlTemplate();

        ninjaHtmlTemplate.append(
                """
                <header>
                <p>That's the funky header, duderino!</p>
                </header>
                """
        );
        
        return ninjaHtmlTemplate;
    }

}
