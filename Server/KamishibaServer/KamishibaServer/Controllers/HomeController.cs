using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using KamishibaServer.Models;
using Microsoft.EntityFrameworkCore;

namespace KamishibaServer.Controllers
{
    public class HomeController : KamishibaController
    {
        public HomeController(KamishibaServerContext context) : base(context) { }

        public IActionResult Index()
        {
            return View();
        }

        public IActionResult Error()
        {
            return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
        }
        
        public IActionResult App()
        {
            return View();
        }
    }
}
