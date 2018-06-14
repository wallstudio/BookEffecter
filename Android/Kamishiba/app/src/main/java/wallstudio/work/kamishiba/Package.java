package wallstudio.work.kamishiba;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Package {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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

    public static Package fromYaml(String yamlString) throws IOException, ParseException {
        Yaml yaml = new Yaml();
        Map map = yaml.load(yamlString);
        if(null == map) throw new IOException("Invalid YAML");
        Package pack = new Package();
        pack.uuid = UUID.fromString((String) map.get("UUID"));
        Map book = (Map) map.get("book_info");
        pack.book.uuid = UUID.fromString((String) book.get("UUID"));
        pack.book.id = (String) book.get("id");
        pack.book.title = (String) book.get("title");
        Map author = (Map) book.get("author");
        pack.book.author.uuid = UUID.fromString((String) author.get("UUID"));
        pack.book.author.id = (String) author.get("id");
        pack.book.author.name = (String) author.get("name");
        pack.book.author.contact = (String) author.get("contact");
        pack.book.page_count = (int) book.get("page_count");
        pack.book.publish_date = (Date) book.get("publish_date");
        pack.book.genre = (List<String>) book.get("genre");
        pack.book.sexy = (boolean) book.get("sexy");
        pack.book.vaiolence = (boolean) book.get("vaiolence");
        pack.book.grotesque = (boolean) book.get("grotesque");
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
            pack.audio.add(ua);
        }
        return  pack;
    }
}
