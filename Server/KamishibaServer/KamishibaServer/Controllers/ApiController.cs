using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using KamishibaServer.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using YamlDotNet.Serialization;

namespace KamishibaServer.Controllers
{
    public class ApiController : KamishibaController
    {
        public class IndexTemplate
        {
            public string id { get; set; }
            public string title { get; set; }
            public string author { get; set; }
            public string contact { get; set; }
            public int page_count { get; set; }
            public int audio_count { get; set; }
            public DateTime publish_date { get; set; }
            public List<string> genre { get; set; }
            public bool sexy { get; set; }
            public bool vaiolence { get; set; }
            public bool grotesque { get; set; }
            public bool download_status { get; set; }

            public IndexTemplate(KamishibaServerContext context, Book book)
            {
                id = book.IDName;
                title = book.Title;
                author = book.Auther;
                contact = book.Contact;
                page_count = Directory.GetFiles("wwwroot" + Path.DirectorySeparatorChar + book.IDName, "*.jpg").Length;
                audio_count = context.Audio.Count(a => a.BookID == book.ID);
                publish_date = book.PublishedDate;
                genre = book.Tags.Split(',', StringSplitOptions.RemoveEmptyEntries).Select(t => t.Trim()).ToList();
                sexy = book.Sexy;
                vaiolence = book.Vaiolence;
                grotesque = book.Grotesque;
                download_status = false;
            }
        }

        public class PackageTemplate
        {
            public class Audio
            {
                public string title { get; set; }
                public string author { get; set; }
                public string contact { get; set; }
                public DateTime publish_date { get; set; }
                public bool official { get; set; }
                public double[] track_timing { get; set; }

                public Audio(KamishibaServerContext context, Book book, Models.Audio audio)
                {
                    audio.Register = context.TUser.Single(u => u.ID == audio.RegisterID);
                    title = audio.Title;
                    author = audio.Register.Name;
                    contact = $"https://twitter.com/{audio.Register.ScreenName}";
                    publish_date = audio.PublishedDate;
                    official = audio.RegisterID == book.RegisterID;
                    track_timing = audio.TrackTiming.Trim('[', ']', ' ').Split(',').Select(d => double.Parse(d)).ToArray();
                }
            }

            public string id { get; set; }
            public string title { get; set; }
            public string author { get; set; }
            public string contact { get; set; }
            public int page_count { get; set; }
            public int audio_count { get; set; }
            public DateTime publish_date { get; set; }
            List<string> genre { get; set; }
            bool sexy { get; set; }
            bool vaiolence { get; set; }
            bool grotesque { get; set; }
            bool download_status { get; set; }
            public string description { get; set; }
            public List<Audio> audio { get; set; }

            public PackageTemplate(KamishibaServerContext context, Book book)
            {
                id = book.IDName;
                title = book.Title;
                author = book.Auther;
                contact = book.Contact;
                page_count = Directory.GetFiles("wwwroot" + Path.DirectorySeparatorChar + book.IDName, "*.jpg").Length;
                audio_count = context.Audio.Count(a => a.BookID == book.ID);
                publish_date = book.PublishedDate;
                genre = book.Tags.Split(',', StringSplitOptions.RemoveEmptyEntries).Select(t => t.Trim()).ToList();
                sexy = book.Sexy;
                vaiolence = book.Vaiolence;
                grotesque = book.Grotesque;
                download_status = false;
                description = book.Description;
                audio = new List<Audio>();
                foreach(var a in context.Audio.Where(a=> a.BookID == book.ID))
                {
                    audio.Add(new Audio(context, book, a));
                }
            }
        }

        public ApiController(KamishibaServerContext context) : base(context) { }

        [Route("Api")]
        public string Index()
        {
            return Index(null);
        }

        [Route("Api/{id}")]
        public string Index(string id)
        {
            if (id == null)
            {
                var data = new List<IndexTemplate>();
                foreach (var book in context.Book)
                {
                    data.Add(new IndexTemplate(context, book));
                }
                var serializer = new SerializerBuilder().EmitDefaults().Build();
                return serializer.Serialize(data);
            }
            else
            {
                var book = context.Book.Single(b => b.IDName == id);
                var data = new PackageTemplate(context, book);
                var serializer = new SerializerBuilder().EmitDefaults().Build();
                return serializer.Serialize(data);
            }
        }
    }
}