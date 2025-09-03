package conf;

import org.ninjax.core.Request;
import org.ninjax.core.FilterChain;
import org.ninjax.core.NinjaFilter;
import org.ninjax.core.Result;


public class MyCustomFilter implements NinjaFilter {

    @Override
    public Result doFilter(Request request, FilterChain chain) {
        System.out.println("test!");
        return chain.doFilter(request);
    }
    
}
