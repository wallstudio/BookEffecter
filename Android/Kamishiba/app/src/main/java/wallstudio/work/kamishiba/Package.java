package wallstudio.work.kamishiba;

import android.content.Context;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Package {

    public static final String DOWNLOADED_LIST_PATH = "downloaded.list";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static List<UUID> sDownlowed;

    public Context context;
    public String url;

    public UUID uuid;
    public Book book = new Book();
    public List<Audio> audio = new ArrayList<>();

    public static class Book{
        public UUID uuid;
        public String id;
        public String title;

        public Author author = new Author();

        public int page_count;
        public Date publish_date;
        public List<String> genre = new ArrayList<>();
        public boolean sexy;
        public boolean vaiolence;
        public boolean grotesque;
    }
    public static class Author {
        public UUID uuid;
        public String id;
        public String name;
        public String contact;
    }
    public static class Audio{
        UUID uuid;
        String id;
        String title;
        Author author = new Author();
        Date publish_date;
        boolean official;
        List<Double> track_timing = new ArrayList<>();
    }

    public Package(Context context, String url, UUID uuid){
        this.context = context;
    }

    public void create(){
        // PackageGridAdapter.getView で読み込まれる
        String yamlString;
        if(sDownlowed.indexOf(uuid) < 0){

        }
    }


    private String loadStringFromRemote(String url){
        return "";
    }

    public void fromYaml(String yamlString) throws IOException, ParseException {
        Yaml yaml = new Yaml();
        Map map = yaml.load(yamlString);
        if(null == map) throw new IOException("Invalid YAML");
        uuid = UUID.fromString((String) map.get("UUID"));
        Map book = (Map) map.get("book_info");
        this.book.uuid = UUID.fromString((String) book.get("UUID"));
        this.book.id = (String) book.get("id");
        this.book.title = (String) book.get("title");
        Map author = (Map) book.get("author");
        this.book.author.uuid = UUID.fromString((String) author.get("UUID"));
        this.book.author.id = (String) author.get("id");
        this.book.author.name = (String) author.get("name");
        this.book.author.contact = (String) author.get("contact");
        this.book.page_count = (int) book.get("page_count");
        this.book.publish_date = (Date) book.get("publish_date");
        this.book.genre = (List<String>) book.get("genre");
        this.book.sexy = (boolean) book.get("sexy");
        this.book.vaiolence = (boolean) book.get("vaiolence");
        this.book.grotesque = (boolean) book.get("grotesque");
        List audio = (List) map.get("audio");
        for (Object ao: audio){
            Map am = (Map)ao;
            Package.Audio ua = new Package.Audio();
            ua.uuid = UUID.fromString((String) am.get("UUID"));
            ua.id = (String) am.get("id");
            ua.title = (String) am.get("title");
            Map ama = (Map) am.get("author");
            ua.author.uuid = UUID.fromString((String) ama.get("UUID"));
            ua.author.id = (String) ama.get("id");
            ua.author.name = (String) ama.get("name");
            ua.author.contact = (String) ama.get("contact");
            ua.publish_date = (Date) am.get("publish_date");
            ua.official = (boolean) am.get("official");
            ua.track_timing = (List<Double>) am.get("track_timing");
            this.audio.add(ua);
        }
    }
}
