package views;

import org.ninjax.htmltemplate.NinjaHtmlTemplate;

public class FooterHtml {

    public static NinjaHtmlTemplate render() {
        NinjaHtmlTemplate ninjaHtmlTemplate = new NinjaHtmlTemplate();

        ninjaHtmlTemplate.append(
                """
                <footer>
                <p>That's the funky footer, dude!</p>
                </footer>
                """
        );

        return ninjaHtmlTemplate;
    }

}
