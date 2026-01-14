package conf;

import org.ninja.core.Request;
import org.ninja.core.FilterChain;
import org.ninja.core.NinjaFilter;
import org.ninja.core.Result;


public class MyCustomFilter implements NinjaFilter {

    @Override
    public Result doFilter(Request request, FilterChain chain) {
        System.out.println("test!");
        return chain.doFilter(request);
    }
    
}
