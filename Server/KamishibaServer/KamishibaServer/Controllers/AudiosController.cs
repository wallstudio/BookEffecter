using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using KamishibaServer.Models;
using Microsoft.AspNetCore.Http;
using GroovyCodecs.Mp3;
using GroovyCodecs.Types;
using GroovyCodecs.WavFile;

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
        public async Task<IActionResult> Create(int? bid)
        {
            if (bid == null) return NotFound();
            var book = await _context.Book.SingleOrDefaultAsync(m => m.ID == bid);
            if (book == null) return NotFound();

            var twitterUser = _context.User.SingleOrDefault(user => user.ID == TwitterUser.GetID(User));

            var audio = new Audio()
            {
                RegisterID = twitterUser.ID,
                BookID = (int)bid,
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
        public async Task<IActionResult> Create([Bind("ID,RegisterID,BookID,Title,TrackTiming," +
            "PublishedDate,LastUpdate,CreatedUpdate")] Audio audio, IFormFile audioFiles)
        {
            string audioErrorMessage = "";

            if (ModelState.IsValid)
            {
                // 画像の検証
                if (null == audio)
                    audioErrorMessage += "画像は1～60枚の範囲で登録してください。";

                var book = _context.Book.SingleOrDefault(b => b.ID == audio.BookID);
                audioErrorMessage += await SaveAudioAsync(book.IDName, audio.ID.ToString(), audioFiles);

                if (audioErrorMessage == "")
                {
                    // エントリーの正規化
                    if (audio.PublishedDate == null)
                        audio.PublishedDate = DateTime.Now.Date;
                    audio.CreatedUpdate = DateTime.Now;
                    audio.LastUpdate = DateTime.Now;

                    // DBに登録
                    _context.Add(audio);
                    await _context.SaveChangesAsync();
                    return RedirectToAction(nameof(Index));
                }
            }
            ViewData["audio_error_message"] = audioErrorMessage;
            // 修正を促す
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
        
        private async Task<string> SaveAudioAsync(string packId, string id, IFormFile audio)
        {
            var dir = "wwwroot"
                + Path.DirectorySeparatorChar + packId
                + Path.DirectorySeparatorChar + "audio";

            if (!Directory.Exists(dir))
                Directory.CreateDirectory(dir);

            //if (Directory.GetFiles(dir).Length > 0) // Always false
            //    return "重複があります。IDを変えてください。";
            
            var path = dir + Path.DirectorySeparatorChar + id + ".mp3"; // wavの可能性？
            try
            {
                if (audio.Length <= 0)
                    throw new Exception($"ファイルの中身がありません。（{packId}_{id}）");
                if (audio.Length > 20000000)
                    throw new Exception($"ファイルが20MBを超えています。（{packId}_{id}）");

                using (var stream = new FileStream(path, FileMode.Create))
                    await audio.CopyToAsync(stream);

                try
                {
                    var bitrate = 64;
                    var encoder = new Mp3Encoder(new AudioFormat(),
                        bitrate, Mp3Encoder.CHANNEL_MODE_MONO, Mp3Encoder.QUALITY_MIDDLE, true);
                    var audioFile = new WavReader();
                    audioFile.OpenFile(path);
                    var srcFormat = audioFile.GetFormat();
                    encoder.SetFormat(srcFormat, srcFormat);
                    var inBuffer = audioFile.readWav();
                    var outBuffer = new byte[inBuffer.Length];

                    var len = encoder.EncodeBuffer(inBuffer, 0, inBuffer.Length, outBuffer);
                    encoder.Close();

                    using (var outfile = System.IO.File.Create(
                        Path.GetDirectoryName(path) + Path.DirectorySeparatorChar + id + ".mp3"))
                    {
                        outfile.Write(outBuffer, 0, len);
                    }

                    Normalize(path);
                }
                catch (IOException e)
                {
                    throw new Exception($"変換できませんでした。管理者にお問い合わせください。（{packId}_{id}）");
                }
                catch(Exception e)
                {
                    throw new Exception($"ファイルが壊れている可能性があります。（{packId}_{id}）");
                }
                finally
                {
                    if (System.IO.File.Exists(path))
                        System.IO.File.Delete(path);
                }
            }
            catch (IOException e)
            {
                Directory.Delete(dir, true);
                return $"保存できませんでした。管理者にお問い合わせください。（{packId}_{id}）";
            }
            catch (OutOfMemoryException e)
            {
                Directory.Delete(dir, true);
                return $"画像ファイルではないようです。もしくは破損している可能性があります。（{packId}_{id}）";
            }
            catch (Exception e)
            {
                Directory.Delete(dir, true);
                return e.Message;
            }

            return "";
        }

        private void Normalize(string path)
        {

        }
    }
}
