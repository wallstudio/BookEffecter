using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using CoreTweet;

namespace KamishibaServer.Controllers
{
    public class AuthenticationController : Controller
    {
        private readonly IAuthenticationSchemeProvider authenticationSchemeProvider;
        public AuthenticationController(IAuthenticationSchemeProvider authenticationSchemeProvider)
        {
            this.authenticationSchemeProvider = authenticationSchemeProvider;
        }

        public async Task<IActionResult> Login()
        {
            var allSchemeProvider = (await authenticationSchemeProvider.GetAllSchemesAsync())
                .Select(n => n.DisplayName).Where(n => !String.IsNullOrEmpty(n));

            return View(allSchemeProvider);
        }

        public IActionResult SignIn(String provider)
        {
            return Challenge(new AuthenticationProperties { RedirectUri = "/Authentication/ExternalLoginCallback" }, provider);
            //return Challenge(new AuthenticationProperties { RedirectUri = "/" }, provider);
        }

        public async Task<IActionResult> ExternalLoginCallback(string returnUrl = null, string remoteError = null)
        {
            var accessToken = User.Claims.FirstOrDefault(x => x.Type == "AccessToken")?.Value;
            var accessSecret = User.Claims.FirstOrDefault(x => x.Type == "AccessTokenSecret")?.Value;
            var accessUserId = User.Claims.FirstOrDefault(x => x.Type == "UserId")?.Value;
            var accessScreen = User.Claims.FirstOrDefault(x => x.Type == "ScreenName")?.Value;

            var tokens = Tokens.Create(
                Secret.TwitterConsumerKey,
                Secret.TwitterConsumerSecret,
                accessToken, accessSecret);
            //var r = await tokens.Statuses.UpdateAsync("ﾏｷﾏｷｶﾜｲｲﾔｯﾀｰ!!");
            var s = await tokens.Account.UpdateProfileAsync();

            var isAuthenticated = User.Identity.IsAuthenticated;

            if (remoteError != null)
                return RedirectToAction(nameof(Login));
            if (!isAuthenticated)
                return RedirectToAction(nameof(Login));
                
            ViewData.Add("nicname", s.Name);
            return View();
        }


        public async Task<IActionResult> SignOut()
        {
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            return RedirectToAction("Index", "Home");
        }
    }
}