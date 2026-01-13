package views;

import org.juckula.JuckulaCompositionTemplate;

public class LayoutHtml {

    public JuckulaCompositionTemplate render(String title, JuckulaCompositionTemplate innerHtml) {
        
        JuckulaCompositionTemplate template = new JuckulaCompositionTemplate();

        template.html("<html>");
        template.html("<head><title>", JuckulaCompositionTemplate.escapeUnsafe(title), "</title></head>");
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
