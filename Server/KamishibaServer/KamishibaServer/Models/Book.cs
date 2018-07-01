using Microsoft.AspNetCore.Http;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;

namespace KamishibaServer.Models
{
    public class Book
    {
        [Key]
        public int ID { get; set; }
        [ForeignKey("User")]
        public long RegisterID { get; set; }

        [DisplayName("ID")]
        [Required]
        [MaxLength(40)]
        [RegularExpression(@"[a-zA-Z0-9_-~]+\.[a-zA-Z0-9_-~]+")]
        public string IDName { get; set; }
        [DisplayName("本のタイトル")]
        [Required]
        [MaxLength(40)]
        public string Title { get; set; }
        [DisplayName("著者のペンネーム")]
        [Required]
        [MaxLength(40)]
        public string Auther { get; set; }
        [DisplayName("連絡先")]
        [Required]
        public string Contact { get; set; }
        [DisplayName("ページ数")]
        [Required]
        public int PageCount { get; set; }
        // public int AudioCount;
        [DisplayName("発行日")]
        [Required]
        [DataType(DataType.Date)]
        [DisplayFormat(DataFormatString = "{0:yyyy-MM-dd}", ApplyFormatInEditMode = true)]
        public DateTime PublishedDate { get; set; }
        [DisplayName("タグ")]
        [RegularExpression(@"(^ *$|^( *[^!""#$%&'()\*\+\-\.,\/:;<=>?@\[\\\]^ `{|}~]+ *)( *, *[^!""#$%&'()\*\+\-\.,\/:;<=>?@\[\\\]^ `{|}~]+)* *$)")]
        [MaxLength(100)]
        public string Tags { get; set; }
        [DisplayName("性的表現の有無")]
        public bool Sexy { get; set; }
        [DisplayName("暴力表現の有無")]
        public bool Vaiolence { get; set; }
        [DisplayName("グロテスクな表現の有無")]
        public bool Grotesque { get; set; }
        [DisplayName("説明")]
        public string Description { get; set; }

        public DateTime LastUpdate { get; set; }
        public DateTime CreatedUpdate { get; set; }

        public List<IFormFile> Images;

        public Book() { }
    }
}