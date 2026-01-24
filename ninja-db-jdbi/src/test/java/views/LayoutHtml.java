package views;

import org.ninjax.htmltemplate.Html;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;

public class LayoutHtml {

    public NinjaHtmlTemplate render(String title, NinjaHtmlTemplate innerHtml) {

        NinjaHtmlTemplate template = new NinjaHtmlTemplate();

        template.appendHtml("<html>");
        template.appendHtml("<head><title>");
        template.appendHtml(title);
        template.appendHtml("</title></head>");
        template.appendHtml("<body>");

        template.append(HeaderHtml.render());

        for (int i = 0; i < 10; i++) {
            template.append(LinkTag.render("title" + i, "href" + i));
        }

        template.append(innerHtml);

        template.append(FooterHtml.render());

        template.append("</body>");
        template.append("</html>");

        return template;

    }

}
