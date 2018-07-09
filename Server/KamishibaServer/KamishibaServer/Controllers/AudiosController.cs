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
using System.Text;

namespace KamishibaServer.Controllers
{
    public class AudiosController : KamishibaController
    {
        public const int ACCESSBLE = TUser.POWER_NORMAL;
        public const int SUPER_EDITABLE = TUser.POWER_MODERATOR;

        public AudiosController(KamishibaServerContext context) : base(context) { }

        // GET: Audios
        public async Task<IActionResult> Index()
        {
            return View(await context.Audio.ToListAsync());
        }

        // GET: Audios/Details/5
        public async Task<IActionResult> Details(int? id)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();
            if (id == null) return NotFound();

            var audio = await context.Audio.SingleOrDefaultAsync(m => m.ID == id);
            if (audio == null) return NotFound();
            if (audio.RegisterID != TUser.ID && TUser.Power > SUPER_EDITABLE) return NotAllowd();

            audio.Parent = await context.Book.SingleOrDefaultAsync(b => b.ID == audio.BookID);
            audio.Register = await context.TUser.SingleOrDefaultAsync(u => u.ID == audio.RegisterID);

            return View(audio);
        }

        // GET: Audios/Create
        public async Task<IActionResult> Create(int? bid)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();

            if (bid == null) return NotFound();
            var book = await context.Book.SingleOrDefaultAsync(m => m.ID == bid);
            if (book == null) return NotFound();

            // 3rdの許可がされていない本では3rdを弾く
            if (TUser.Power > SUPER_EDITABLE &&book.LimitedOffcial && book.RegisterID != TUser.ID)
                return NotAllowd();

            var audio = new Audio()
            {
                RegisterID = TUser.ID,
                BookID = (int)bid,
                TrackTiming = "[0,1]",
                PublishedDate = DateTime.Now.Date,
                LastUpdate = DateTime.Now,
                CreatedUpdate = DateTime.Now
            };

            audio.Parent = book;
            return View(audio);
        }

        // POST: Audios/Create
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("ID,RegisterID,BookID,Title,TrackTiming," +
            "PublishedDate,LastUpdate,CreatedUpdate")] Audio audio, IFormFile audioFiles)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();

            audio.Parent = await context.Book.SingleOrDefaultAsync(m => m.ID == audio.BookID);
            if (audio.Parent == null) return NotFound();

            // 3rdの許可がされていない本では3rdを弾く
            if (TUser.Power > SUPER_EDITABLE && audio.Parent.LimitedOffcial && audio.Parent.RegisterID != TUser.ID)
                return NotAllowd();

            string audioErrorMessage = "";

            if (ModelState.IsValid)
            {
                // 画像の検証
                if (null == audio)
                    audioErrorMessage += "画像は1～60枚の範囲で登録してください。";
                // エントリーの正規化
                if (audio.PublishedDate == null)
                    audio.PublishedDate = DateTime.Now.Date;
                audio.CreatedUpdate = DateTime.Now;
                audio.LastUpdate = DateTime.Now;
                try
                {
                    await TUser.TwitterTokens.Statuses.UpdateWithMediaAsync(
                            status => $"「かみしば」で同人誌に音声を登録しました！\n\n{audio.Title} ( {audio.Parent.Title} )\n{Request.Scheme}://{Request.Host}/Books/Details/{audio.Parent.ID}",
                            media => new FileInfo("wwwroot"
                            + Path.DirectorySeparatorChar + "packages"
                            + Path.DirectorySeparatorChar + audio.Parent.IDName
                            + Path.DirectorySeparatorChar + "0.jpg"));
                }
                catch { }
                // DBに登録
                audio = context.Add(audio).Entity;
                await context.SaveChangesAsync();
                
                audioErrorMessage += await SaveAudioAsync(audio.Parent.IDName, audio.ID.ToString(), audioFiles);

                if(audioErrorMessage == "")
                    return Redirect($"/Books/Details/{audio.Parent.ID}");
                else
                {
                    context.Remove(audio);
                    await context.SaveChangesAsync();
                } 
            }
            ViewData["audio_error_message"] = audioErrorMessage;
            // 修正を促す
            return View(audio);
        }

        // GET: Audios/Edit/5
        public async Task<IActionResult> Edit(int? id)
        {
            // 一般ユーザーは編集できない
            if (TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (id == null) return NotFound();
            var audio = await context.Audio.SingleOrDefaultAsync(m => m.ID == id);
            if (audio == null) return NotFound();
            return View(audio);
        }

        // POST: Audios/Edit/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, [Bind("ID,RegisterID,BookID,Title,TrackTiming," +
            "PublishedDate,LastUpdate,CreatedUpdate")] Audio audio)
        {
            // 一般ユーザーは編集できない
            if (TUser.Power > SUPER_EDITABLE) return NotAllowd();

            if (id != audio.ID) return NotFound();
            if (ModelState.IsValid)
            {
                try
                {
                    context.Update(audio);
                    await context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!AudioExists(audio.ID))
                        return NotFound();
                    else
                        throw;
                }
                return RedirectToAction(nameof(Index));
            }
            return View(audio);
        }

        // GET: Audios/Delete/5
        public async Task<IActionResult> Delete(int? id)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();

            if (id == null) return NotFound();
            var audio = await context.Audio.SingleOrDefaultAsync(m => m.ID == id);
            // 本人（＋管理者）以外は弾く
            if (TUser.Power > SUPER_EDITABLE && TUser.ID != audio.RegisterID) NotAllowd();
            if (audio == null) return NotFound();

            audio.Parent = await context.Book.SingleOrDefaultAsync(b => b.ID == audio.BookID);
            
            return View(audio);
        }

        // POST: Audios/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();

            var audio = await context.Audio.SingleOrDefaultAsync(m => m.ID == id);
            // 本人（＋管理者）以外は弾く
            if (TUser.Power > SUPER_EDITABLE && TUser.ID != audio.RegisterID) NotAllowd();
            context.Audio.Remove(audio);
            await context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
        }

        private bool AudioExists(int id)
        {
            return context.Audio.Any(e => e.ID == id);
        }

        private const string MP3_EXTENTION = ".mp3"; 
        private const string WAV_EXTENTION = ".wav"; 
        private const string TMP_EXTENTION = ".tmp";
        private async Task<string> SaveAudioAsync(string packId, string id, IFormFile audio)
        {
            var dir = "wwwroot"
                + Path.DirectorySeparatorChar + "packages"
                + Path.DirectorySeparatorChar + packId;

            if (!Directory.Exists(dir))
                Directory.CreateDirectory(dir);

            var dirName = dir + Path.DirectorySeparatorChar + id;
            if (System.IO.File.Exists(dirName + MP3_EXTENTION))
                return "重複があります。トップページからやり直してください。";

            try
            {
                if (audio.Length <= 0)
                    throw new Exception($"ファイルの中身がありません。（{packId}_{id}）");
                if (audio.Length > 20000000)
                    throw new Exception($"ファイルが20MBを超えています。（{packId}_{id}）");

                string extension;
                using (var stream = new FileStream(dirName + TMP_EXTENTION, FileMode.Create))
                {
                    await audio.CopyToAsync(stream);
                    stream.Seek(0, SeekOrigin.Begin);
                    extension = CheckMp3OrWave(stream);
                }

                if (extension == WAV_EXTENTION)
                {
                    try
                    {
                        ComvertWav2Mp3(dirName);
                    }
                    catch (IOException)
                    {
                        throw new Exception($"変換できませんでした。管理者にお問い合わせください。（{packId}_{id}）");
                    }
                    catch (Exception)
                    {
                        throw new Exception($"ファイルが壊れている可能性があります。（{packId}_{id}）");
                    }
                }
                else if(extension == MP3_EXTENTION)
                {
                    System.IO.File.Copy(dirName + TMP_EXTENTION, dirName + MP3_EXTENTION);
                }
            }
            catch (IOException)
            {
                Directory.Delete(dir, true);
                return $"保存できませんでした。管理者にお問い合わせください。（{packId}_{id}）";
            }
            catch (OutOfMemoryException)
            {
                Directory.Delete(dir, true);
                return $"画像ファイルではないようです。もしくは破損している可能性があります。（{packId}_{id}）";
            }
            catch (Exception e)
            {
                Directory.Delete(dir, true);
                return e.Message;
            }
            finally
            {
                if (System.IO.File.Exists(dirName + TMP_EXTENTION))
                    System.IO.File.Delete(dirName + TMP_EXTENTION);
            }

            return "";
        }

        private static void ComvertWav2Mp3(string dirName)
        {
            var bitrate = 64;
            var encoder = new Mp3Encoder(new AudioFormat(),
                bitrate, Mp3Encoder.CHANNEL_MODE_MONO, Mp3Encoder.QUALITY_MIDDLE, true);
            var audioFile = new WavReader();
            audioFile.OpenFile(dirName + TMP_EXTENTION);
            var srcFormat = audioFile.GetFormat();
            encoder.SetFormat(srcFormat, srcFormat);
            var inBuffer = audioFile.readWav();
            var outBuffer = new byte[inBuffer.Length];

            var len = encoder.EncodeBuffer(inBuffer, 0, inBuffer.Length, outBuffer);
            encoder.Close();

            using (var outfile = System.IO.File.Create(dirName + MP3_EXTENTION))
                outfile.Write(outBuffer, 0, len);
        }

        private string CheckMp3OrWave(Stream stream)
        {
            var current = stream.Position;
            stream.Seek(0, SeekOrigin.Begin);
            var sign = new byte[3];
            stream.Read(sign, 0, sign.Length);
            var signStr = Encoding.ASCII.GetString(sign);

            if (signStr == "ID3") return MP3_EXTENTION;

            if(signStr == "RIF")
            {
                stream.Seek(8, SeekOrigin.Begin);
                stream.Read(sign, 0, sign.Length);
                signStr = Encoding.ASCII.GetString(sign);
                if (signStr == "WAV") return WAV_EXTENTION;
            }

            throw new Exception("未知のファイル");
        }
    }
}
