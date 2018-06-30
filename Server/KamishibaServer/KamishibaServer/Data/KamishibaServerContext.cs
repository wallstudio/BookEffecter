using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace KamishibaServer.Models
{
    public class KamishibaServerContext : DbContext
    {
        public KamishibaServerContext(DbContextOptions<KamishibaServerContext> options)
            : base(options) { }
        
        public DbSet<KamishibaServer.Models.TwitterUser> User { get; set; }

        public static void Initialize(IServiceProvider serviceProvider)
        {
            using (var context = new KamishibaServerContext(
                serviceProvider.GetRequiredService<DbContextOptions<KamishibaServerContext>>()))
            {
                // Look for any movies.
                if (context.User.Any())
                {
                    return;   // DB has been seeded
                }

                context.User.AddRange(new TwitterUser() {
                    ID = 5000,
                    ScreenName = "example_084",
                    Name = "まき☆",
                    Url = "https://twitter.com/yukawallstudio",
                    LastUpdate = DateTime.Now,
                    AccessToken = "mkmkmkmkmmmkmkmk",
                    AccessSecret = "MKMKMKMKMKMKMKMKMMKMMMKK"
            });
                context.SaveChanges();
            }
        }
    }
}
