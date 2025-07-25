package views;

import org.juckula.JuckulaCompositionTemplate;

public class HeaderHtml {

    public static JuckulaCompositionTemplate render() {
        JuckulaCompositionTemplate juckula2Template = new JuckulaCompositionTemplate();

        juckula2Template.html(
                """
                <header>
                <p>That's the funky header, dude!</p>
                </header>
                """
        );
        
        return juckula2Template;
    }

}
