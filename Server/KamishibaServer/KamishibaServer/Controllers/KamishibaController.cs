﻿using KamishibaServer.Models;
using Microsoft.AspNetCore.Mvc;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Threading.Tasks;

namespace KamishibaServer.Controllers
{
    public abstract class KamishibaController: Controller
    {
        protected readonly KamishibaServerContext context;
        private TUser tUser;
        protected TUser TUser { get {
                if (tUser == null)
                {
                    if (User.Identity.IsAuthenticated)
                        tUser = context.TUser.SingleOrDefault(tu => tu.ID == TUser.GetID(User));
                    if (tUser == null)
                        tUser = TUser.Anonymous;
                    ViewData["power"] = tUser.Power;
                }
                return tUser;
            }}

        public KamishibaController(KamishibaServerContext context): base()
        {
            this.context = context;
            
        }

        protected IActionResult NeedLogin()
        {
            TempData["login_redirect"] = Request.Path + Request.QueryString;
            return Redirect("/Authentication/NeedLoginRedirect");
        }

        protected IActionResult Top()
        {
            return Redirect("/");
        }

        protected IActionResult NotAllowd()
        {
            return Redirect("/Authentication/NotAllowd");
        }

        [NonAction]
        public override ViewResult View()
        {
            ViewBag.TUser = TUser;
            ViewBag.Request = Request;
            return base.View();
        }
        [NonAction]
        public override ViewResult View(string viewName, object model)
        {
            ViewBag.Request = Request;
            ViewBag.TUser = TUser;
            return base.View(viewName, model);
        }
        [NonAction]
        public override ViewResult View(object model)
        {
            ViewBag.Request = Request;
            ViewBag.TUser = TUser;
            return base.View(model);
        }
        [NonAction]
        public override ViewResult View(string viewName)
        {
            ViewBag.Request = Request;
            ViewBag.TUser = TUser;
            return base.View(viewName);
        }
    }
}
