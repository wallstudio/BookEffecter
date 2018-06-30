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
using KamishibaServer.Models;
using Microsoft.EntityFrameworkCore;

namespace KamishibaServer.Controllers
{
    public class AuthenticationController : Controller
    {
        private readonly IAuthenticationSchemeProvider authenticationSchemeProvider;
        private readonly KamishibaServerContext _context;

        public AuthenticationController(IAuthenticationSchemeProvider authenticationSchemeProvider, KamishibaServerContext context)
        {
            this.authenticationSchemeProvider = authenticationSchemeProvider;
            _context = context;
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

            var isAuthenticated = User.Identity.IsAuthenticated;

            if (remoteError != null)
                return RedirectToAction(nameof(Login));
            if (!isAuthenticated)
                return RedirectToAction(nameof(Login));

            var c = _context.User.Select(user => user.ID);
            if (!_context.User.Select(user => user.ID).Contains(TwitterUser.GetID(User)))
            {
                _context.Add(new Models.TwitterUser(User));
                _context.SaveChanges();
            }

            var name = await _context.User.SingleOrDefaultAsync(m => m.ID == TwitterUser.GetID(User));
            TempData.Add("info", $"ようこそ {name.Name} 様！ (@{name.ScreenName})");
            return Redirect("/");
        }


        public async Task<IActionResult> SignOut()
        {
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            return RedirectToAction("Index", "Home");
        }
    }
}