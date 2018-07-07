using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;

namespace KamishibaServer.Models
{
    public class Audio
    {
        [Key]
        public int ID { get; set; }
        [ForeignKey("User")]
        public long RegisterID { get; set; }
        [ForeignKey("Book")]
        public int BookID { get; set; }

        [DisplayName("タイトル")]
        [Required]
        [MaxLength(40)]
        public string Title { get; set; }

        // プログラムから生成
        [DisplayName("タイミング")]
        [Required]
        [RegularExpression(@"\[( *[0-9]+(\.[0-9]+)? *)(, *( *[0-9]+(\.[0-9]+)? *))*\]")]
        public string TrackTiming { get; set; }

        public DateTime PublishedDate { get; set; }
        public DateTime LastUpdate { get; set; }
        public DateTime CreatedUpdate { get; set; }

        [NotMapped]
        public TUser Register;
        [NotMapped]
        public Book Parent;
    }
}
