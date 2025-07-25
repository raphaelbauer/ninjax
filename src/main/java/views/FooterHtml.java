package views;

import org.juckula.JuckulaCompositionTemplate;

public class FooterHtml {

    public static JuckulaCompositionTemplate render() {
        JuckulaCompositionTemplate juckula2Template = new JuckulaCompositionTemplate();

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
