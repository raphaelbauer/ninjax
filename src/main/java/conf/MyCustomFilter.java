package conf;

import org.ninjax.core.Context;
import org.ninjax.core.FilterChain;
import org.ninjax.core.NinjaFilter;
import org.ninjax.core.Result;


public class MyCustomFilter implements NinjaFilter {

    @Override
    public Result doFilter(Context context, FilterChain chain) {
        System.out.println("test!");
        return chain.doFilter(context);
    }
    
}
