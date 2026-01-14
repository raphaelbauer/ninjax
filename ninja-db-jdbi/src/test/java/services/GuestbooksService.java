package services;

import java.util.List;
import models.Guestbook;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.customizer.BindBean;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.ninja.db.jdbi.NinjaJdbi;


public class GuestbooksService {
    
    public interface DbServiceInterface {  
        @SqlQuery("SELECT id, email, content FROM guestbooks")
        @UseRowMapper(Guestbook.GuestbookMapper.class)
        List<Guestbook> listGuestBookEntries();
        
        @SqlUpdate("INSERT INTO guestbooks (email, content) VALUES (:email, :content)")
        void createGuestbook(@BindBean Guestbook guestbook);
    }

    private final Jdbi jdbi;
            
   
    public GuestbooksService(NinjaJdbi ninjaJdbi) {
        this.jdbi = ninjaJdbi.getJdbi("default");
    }

    public List<Guestbook> listGuestBookEntries() {
        return jdbi.open().attach(DbServiceInterface.class).listGuestBookEntries();
    }

    public void createGuestbook(Guestbook guestbook) {
        jdbi.open().attach(DbServiceInterface.class).createGuestbook(guestbook);
    }
    
}
