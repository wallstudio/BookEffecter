using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using KamishibaServer.Models;

namespace KamishibaServer.Models
{
    public class KamishibaServerContext : DbContext
    {
        public KamishibaServerContext(DbContextOptions<KamishibaServerContext> options)
            : base(options) { }
        
        public DbSet<KamishibaServer.Models.TUser> TUser { get; set; }

        public static void Initialize(IServiceProvider serviceProvider)
        {
            using (var context = new KamishibaServerContext(
                serviceProvider.GetRequiredService<DbContextOptions<KamishibaServerContext>>()))
            {
                // DB Initialize;

                context.SaveChanges();
            }
        }

        public DbSet<KamishibaServer.Models.Book> Book { get; set; }

        public DbSet<KamishibaServer.Models.Audio> Audio { get; set; }
    }
}
