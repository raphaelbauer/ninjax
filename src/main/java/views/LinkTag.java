package views;

import org.juckula.JuckulaCompositionTemplate;

public class LinkTag {

    public static JuckulaCompositionTemplate render(String name, String href) {

        JuckulaCompositionTemplate juckula2Template = new JuckulaCompositionTemplate();
        juckula2Template.html("<a href='", JuckulaCompositionTemplate.escapeUnsafe(href), "'>", JuckulaCompositionTemplate.escapeUnsafe(name), "</a>");
        return juckula2Template;
    }

}
