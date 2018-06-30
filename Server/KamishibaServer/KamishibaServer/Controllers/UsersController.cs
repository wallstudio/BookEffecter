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
    public class UsersController : Controller
    {
        public const int EDITABLE_POWER = TwitterUser.POWER_MODERATOR;

        public const long ADMIN_ID = 770906581110095872L;

        private readonly KamishibaServerContext _context;

        public UsersController(KamishibaServerContext context)
        {
            _context = context;
        }

        // GET: Users
        public async Task<IActionResult> Index()
        {
            
            if(_context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
                return View(await _context.User.ToListAsync());
            else
                return Redirect("/");
        }

        // GET: Users/Details/5
        public async Task<IActionResult> Details(long? id)
        {
            if(_context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
            {
                if (id == null)
                {
                    return NotFound();
                }

                var user = await _context.User
                    .SingleOrDefaultAsync(m => m.ID == id);
                if (user == null)
                {
                    return NotFound();
                }

                return View(user);
            }
            return Redirect("/");
        }

        // GET: Users/Create
        public IActionResult Create()
        {
            if(_context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
                return View();
            else
                return Redirect("/");
        }

        // POST: Users/Create
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("ID,ScreenName,Name,Url")] TwitterUser user)
        {
            if(_context.User.SingleOrDefault(_user => _user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
            {
                if (ModelState.IsValid)
                {
                    _context.Add(user);
                    await _context.SaveChangesAsync();
                    return RedirectToAction(nameof(Index));
                }
                return View(user);
            }
            return Redirect("/");
        }

        // GET: Users/Edit/5
        public async Task<IActionResult> Edit(long? id)
        {
            if(_context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
            {
                if (id == null)
                {
                    return NotFound();
                }

                var user = await _context.User.SingleOrDefaultAsync(m => m.ID == id);
                if (user == null)
                {
                    return NotFound();
                }
                return View(user);
            }
            return Redirect("/");
        }

        // POST: Users/Edit/5
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(long id, [Bind("ID,ScreenName,Name,Url")] TwitterUser user)
        {
            if(_context.User.SingleOrDefault(_user => _user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
            {
                if (id != user.ID)
                {
                    return NotFound();
                }

                if (ModelState.IsValid)
                {
                    try
                    {
                        _context.Update(user);
                        await _context.SaveChangesAsync();
                    }
                    catch (DbUpdateConcurrencyException)
                    {
                        if (!UserExists(user.ID))
                        {
                            return NotFound();
                        }
                        else
                        {
                            throw;
                        }
                    }
                    return RedirectToAction(nameof(Index));
                }
                return View(user);
            }
            return Redirect("/");
        }

        // GET: Users/Delete/5
        public async Task<IActionResult> Delete(long? id)
        {
            if(_context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
            {
                if (id == null)
                {
                    return NotFound();
                }

                var user = await _context.User
                    .SingleOrDefaultAsync(m => m.ID == id);
                if (user == null)
                {
                    return NotFound();
                }

                return View(user);
            }
            return Redirect("/");
        }

        // POST: Users/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(long id)
        {
            if(_context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User)).Power <= EDITABLE_POWER)
            {
                var user = await _context.User.SingleOrDefaultAsync(m => m.ID == id);
                _context.User.Remove(user);
                await _context.SaveChangesAsync();
                return RedirectToAction(nameof(Index));
            }
            return Redirect("/");
        }

        private bool UserExists(long id)
        {
            return _context.User.Any(e => e.ID == id);
        }
    }
}
