package views;

import org.ninja.htmltemplate.NinjaHtmlTemplate;

public class LayoutHtml {

    public NinjaHtmlTemplate render(String title, NinjaHtmlTemplate innerHtml) {
        
        NinjaHtmlTemplate template = new NinjaHtmlTemplate();

        template.html("<html>");
        template.html("<head><title>", NinjaHtmlTemplate.escapeUnsafe(title), "</title></head>");
        template.html("<body>");

        template.html(HeaderHtml.render());

        for (int i = 0; i < 10; i++) {
            template.html(LinkTag.render("title" + i, "href" + i));
        }

        template.html(innerHtml);

        template.html(FooterHtml.render());

        template.html("</body>");
        template.html("</html>");
        
        return template;

    }

}
