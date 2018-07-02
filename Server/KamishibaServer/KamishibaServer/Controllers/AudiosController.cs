using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using KamishibaServer.Models;

namespace KamishibaServer.Controllers
{
    public class AudiosController : Controller
    {
        private readonly KamishibaServerContext _context;

        public AudiosController(KamishibaServerContext context)
        {
            _context = context;
        }

        // GET: Audios
        public async Task<IActionResult> Index()
        {
            return View(await _context.Audio.ToListAsync());
        }

        // GET: Audios/Details/5
        public async Task<IActionResult> Details(int? id)
        {
            if (id == null)
            {
                return NotFound();
            }

            var audio = await _context.Audio
                .SingleOrDefaultAsync(m => m.ID == id);
            if (audio == null)
            {
                return NotFound();
            }

            return View(audio);
        }

        // GET: Audios/Create
        public async Task<IActionResult> Create(int? id)
        {
            if (id == null) return NotFound();
            var book = await _context.Book.SingleOrDefaultAsync(m => m.ID == id);
            if (book == null) return NotFound();

            var twitterUser = _context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User));

            var audio = new Audio()
            {
                RegisterID = twitterUser.ID,
                BookID = (int)id,
                TrackTiming = "[0,1]",
                PublishedDate = DateTime.Now.Date,
                LastUpdate = DateTime.Now,
                CreatedUpdate = DateTime.Now
            };

            ViewData["book"] = book;
            return View(audio);
        }

        // POST: Audios/Create
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("ID,RegisterID,BookID,Title,TrackTiming,PublishedDate,LastUpdate,CreatedUpdate")] Audio audio)
        {
            if (ModelState.IsValid)
            {
                _context.Add(audio);
                await _context.SaveChangesAsync();
                return RedirectToAction(nameof(Index));
            }
            return View(audio);
        }

        // GET: Audios/Edit/5
        public async Task<IActionResult> Edit(int? id)
        {
            if (id == null)
            {
                return NotFound();
            }

            var audio = await _context.Audio.SingleOrDefaultAsync(m => m.ID == id);
            if (audio == null)
            {
                return NotFound();
            }
            return View(audio);
        }

        // POST: Audios/Edit/5
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, [Bind("ID,RegisterID,BookID,Title,TrackTiming,PublishedDate,LastUpdate,CreatedUpdate")] Audio audio)
        {
            if (id != audio.ID)
            {
                return NotFound();
            }

            if (ModelState.IsValid)
            {
                try
                {
                    _context.Update(audio);
                    await _context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!AudioExists(audio.ID))
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
            return View(audio);
        }

        // GET: Audios/Delete/5
        public async Task<IActionResult> Delete(int? id)
        {
            if (id == null)
            {
                return NotFound();
            }

            var audio = await _context.Audio
                .SingleOrDefaultAsync(m => m.ID == id);
            if (audio == null)
            {
                return NotFound();
            }

            return View(audio);
        }

        // POST: Audios/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            var audio = await _context.Audio.SingleOrDefaultAsync(m => m.ID == id);
            _context.Audio.Remove(audio);
            await _context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
        }

        private bool AudioExists(int id)
        {
            return _context.Audio.Any(e => e.ID == id);
        }
    }
}
