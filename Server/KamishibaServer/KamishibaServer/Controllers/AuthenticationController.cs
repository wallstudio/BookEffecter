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
using CoreTweet;

namespace KamishibaServer.Controllers
{
    public class AuthenticationController : KamishibaController
    {
        private readonly IAuthenticationSchemeProvider authenticationSchemeProvider;

        public AuthenticationController(IAuthenticationSchemeProvider authenticationSchemeProvider, KamishibaServerContext context):base(context)
        {
            this.authenticationSchemeProvider = authenticationSchemeProvider;
        }

        public IActionResult NeedLoginRedirect()
        {
            return View();
        }

        public new IActionResult NotAllowd()
        {
            return View();
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

            var c = context.TUser.Select(user => user.ID);
            if (!context.TUser.Select(user => user.ID).Contains(TUser.GetID(User)))
            {
                var tuser = new TUser(User);
                await tuser.TwitterTokens.Statuses.UpdateAsync($"「かみしば」を始めました！\n\n {Request.Scheme}://{Request.Host}");
                context.Add(tuser);
                context.SaveChanges();
            }

            var name = await context.TUser.SingleOrDefaultAsync(m => m.ID == TUser.GetID(User));
            TempData["info"] = $"ようこそ {name.Name} 様！ (@{name.ScreenName})";

            if (TempData["login_redirect"] != null)
            {
                var target = TempData["login_redirect"].ToString();
                TempData["login_redirect"] = null;
                return Redirect(target);
            }
            else
                return Redirect("/");
        }
        
        public async Task<IActionResult> SignOut()
        {
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            return RedirectToAction("Index", "Home");
        }
    }
}