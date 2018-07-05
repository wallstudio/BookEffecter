using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using KamishibaServer.Models;
using System.Security.Claims;

namespace KamishibaServer.Controllers
{
    public class UsersController : KamishibaController
    {
        public const int SUPER_EDITABLE = TUser.POWER_MODERATOR;

        public UsersController(KamishibaServerContext context) : base(context) { }

        // GET: Users
        public async Task<IActionResult> Index()
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            return View(await context.TUser.ToListAsync());
        }

        // GET: Users/Details/5
        public async Task<IActionResult> Details(long? id)
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (id == null) return NotFound();
            var user = await context.TUser.SingleOrDefaultAsync(m => m.ID == id);
            if (user == null) return NotFound();
            return View(user);
        }

        // GET: Users/Create
        public IActionResult Create()
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            return View();
        }

        // POST: Users/Create
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("Power,ID,ScreenName,Name,Url")] TUser user)
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (ModelState.IsValid)
            {
                context.Add(user);
                await context.SaveChangesAsync();
                return RedirectToAction(nameof(Index));
            }
            return View(user);
        }

        // GET: Users/Edit/5
        public async Task<IActionResult> Edit(long? id)
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (id == null) return NotFound();
            var user = await context.TUser.SingleOrDefaultAsync(m => m.ID == id);
            if (user == null) return NotFound();
            return View(user);
        }

        // POST: Users/Edit/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(long id, [Bind("Power,ID,ScreenName,Name,Url")] TUser user)
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (id != user.ID) return NotFound();
            if (ModelState.IsValid)
            {
                try
                {
                    context.Update(user);
                    await context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!UserExists(user.ID)) return NotFound();
                    else throw;
                }
                return RedirectToAction(nameof(Index));
            }
            return View(user);
            
        }

        // GET: Users/Delete/5
        public async Task<IActionResult> Delete(long? id)
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (id == null) return NotFound();
            var user = await context.TUser.SingleOrDefaultAsync(m => m.ID == id);
            if (user == null) return NotFound();

            return View(user);
        }

        // POST: Users/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(long id)
        {
            if(TUser.Power > SUPER_EDITABLE) return NotAllowd();

            var user = await context.TUser.SingleOrDefaultAsync(m => m.ID == id);
            context.TUser.Remove(user);
            await context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
            
        }

        private bool UserExists(long id)
        {
            return context.TUser.Any(e => e.ID == id);
        }
    }
}
