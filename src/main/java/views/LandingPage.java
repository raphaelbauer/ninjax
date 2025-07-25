package views;

import org.juckula.JuckulaCompositionTemplate;


public class LandingPage {
    
    public static JuckulaCompositionTemplate render(String title) {
        
        LayoutHtml layoutHtml = new LayoutHtml();
        
        JuckulaCompositionTemplate juckula2Template = new JuckulaCompositionTemplate();
        juckula2Template.html("<div class='main'><p>a message</p></div>");
        
        return layoutHtml.render(title, juckula2Template);   
    }

}
