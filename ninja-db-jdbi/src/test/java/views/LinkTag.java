package views;

import org.ninjax.htmltemplate.NinjaHtmlTemplate;

public class LinkTag {

    public static NinjaHtmlTemplate render(String name, String href) {

        NinjaHtmlTemplate juckula2Template = new NinjaHtmlTemplate();
        juckula2Template.html("<a href='", NinjaHtmlTemplate.escapeUnsafe(href), "'>", NinjaHtmlTemplate.escapeUnsafe(name), "</a>");
        return juckula2Template;
    }

}
