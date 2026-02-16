package com.r10r.ninjax.htmltemplate;


/**
 * This allows to render the rawHtml string as is into the template.
 * Only render values you fully control, otherwise you will get security 
 * incidents and your app is open to XSS attacks.
 * 
 * The good new is that anything else will be escaped by default.
 */
public record Html(String rawHtml) {
    
}
