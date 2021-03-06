﻿using CoreTweet;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Security.Claims;
using System.Threading.Tasks;

namespace KamishibaServer.Models
{
    public class TUser
    {
        public const int POWER_ADMIN = 0;
        public const int POWER_MODERATOR = 100;
        public const int POWER_TRUSTED = 300;
        public const int POWER_NORMAL = 1000;
        public const int POWER_LIMITED = 10000;
        public const int POWER_ANONYMOUSE = 100000;

        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.None)]
        public long ID { get; set; }
        public string ScreenName { get; set; }
        public string Name { get; set; }
        public string Url { get; set; }
        public DateTime LastUpdate { get; set; }
        public DateTime CreatedUpdate { get; set; }
        public string AccessToken { get; set; }
        public string AccessSecret { get; set; }
        public int Power { get; set; }
        
        [NotMapped]
        public Tokens TwitterTokens {
            get
            {
                if (twitterTokens == null)
                    twitterTokens = Tokens.Create(
                            Secret.TwitterConsumerKey, Secret.TwitterConsumerSecret,
                            AccessToken, AccessSecret);
                return twitterTokens;
            }
        }
        private Tokens twitterTokens;

        public TUser() { }
        public TUser(ClaimsPrincipal user)
        {
            AccessToken = user.Claims.FirstOrDefault(x => x.Type == "AccessToken")?.Value;
            AccessSecret = user.Claims.FirstOrDefault(x => x.Type == "AccessTokenSecret")?.Value;

            var tokens = Tokens.Create(
                Secret.TwitterConsumerKey,
                Secret.TwitterConsumerSecret,
                AccessToken, AccessSecret);

            var task = (tokens.Account.UpdateProfileAsync());
            task.Wait();
            var profile = task.Result;

            ID = (long)profile.Id;
            ScreenName = profile.ScreenName;
            Name = profile.Name;
            Url = profile.Url;
            LastUpdate = DateTime.Now;
            CreatedUpdate = DateTime.Now;
            Power = 1000;
        }

        public static long GetID(ClaimsPrincipal user)
        {
            var isInt = long.TryParse(user.Claims.FirstOrDefault(x => x.Type == "UserId")?.Value, out long id);
            return isInt ? id : -1;
        }

        public static TUser Anonymous = new TUser
        {
            ID = -1,
            Name = "Anonymous",
            Power = POWER_ANONYMOUSE
        };
    }
}
