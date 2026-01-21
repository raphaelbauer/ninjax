package views;

import org.ninjax.htmltemplate.NinjaHtmlTemplate;

public class FooterHtml {

    public static NinjaHtmlTemplate render() {
        NinjaHtmlTemplate juckula2Template = new NinjaHtmlTemplate();

        juckula2Template.html(
                """
                <footer>
                <p>That's the funky footer, dude!</p>
                </footer>
                """
        );

        return juckula2Template;
    }

}
