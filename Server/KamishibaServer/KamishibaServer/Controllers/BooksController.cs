using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using KamishibaServer.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;

namespace KamishibaServer.Controllers
{
    public class BooksController : KamishibaController
    {
        public const int ACCESSBLE = TUser.POWER_NORMAL;

        public BooksController(KamishibaServerContext context) : base(context) { }

        // GET: Books
        public async Task<IActionResult> Index()
        {
            return View(await context.Book.ToListAsync());
        }

        // GET: Books/Details/5
        public async Task<IActionResult> Details(int? id)
        {
            if (id == null) return NotFound();
            var book = await context.Book.SingleOrDefaultAsync(m => m.ID == id);
            if (book == null) return NotFound();

            return View(book);
        }

        // GET: Books/Create
        public IActionResult Create()
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();
            
            var book = new Book
            {
                RegisterID = TUser.GetID(User),
                IDName = "",
                LimitedOffcial = false,
                Auther = TUser.Name,
                Contact = $"https://twitter.com/{TUser.ScreenName}",
                PublishedDate = DateTime.Now.Date
            };
            return View(book);
        }

        // POST: Books/Create
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create(
            [Bind("ID,RegisterID,IDName,LimitedOffcial,Title,Auther,Contact,PageCount,PublishedDate," +
                "Tags,Sexy,Vaiolence,Grotesque,Description,LastUpdate,CreatedUpdate,Images")]
            Book book, List<IFormFile> images)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();

            string imageErrorMessage = "";
            string idErrorMessage = "";
            if (ModelState.IsValid)
            {
                book.IDName = book.IDName.Trim();

                // IDNameが半分のチェック
                if (book.IDName.Contains("."))
                    imageErrorMessage += "IDは半角英数とアンダーバーしか使えません。";
                // IDNameの重複チェック
                var duplicate = context.Book.SingleOrDefault(b => b.IDName == book.IDName);
                if (duplicate != null)
                    idErrorMessage += "IDは既に登録されています。";

                if (idErrorMessage == "")
                {
                    // 画像の検証
                    if (null == images || images.Count <= 0)
                    {
                        imageErrorMessage += "画像は1～60枚の範囲で登録してください。";
                    }
                    imageErrorMessage += await SaveImagesAsync(TUser.ScreenName + "." + book.IDName, images);
                    
                    if (imageErrorMessage == "")
                    {
                        // エントリーの正規化
                        if (book.Auther == null || book.Auther == "")
                            book.Auther = TUser.Name;
                        else
                            book.Auther = book.Auther.Trim();

                        if (book.Contact == null || book.Contact == "")
                            book.Contact = $"https://twitter.com/{TUser.ScreenName}";
                        else
                            book.Contact = book.Contact.Trim();

                        if (book.PublishedDate == null)
                            book.PublishedDate = DateTime.Now.Date;

                        book.PageCount = images.Count;
                        book.CreatedUpdate = DateTime.Now;
                        book.LastUpdate = DateTime.Now;
                        book.IDName = TUser.ScreenName + "." + book.IDName;

                        if (book.Tags == null) book.Tags = "";
                        book.Tags = string.Join(",",
                            book.Tags.Split(',').Select(s => s.Trim().Replace(" ", "_")).ToArray());

                        // DBに登録
                        context.Add(book);
                        if (book.Description == null) book.Description = "";
                        else book.Description = book.Description.Trim();
                        await context.SaveChangesAsync();
                        return RedirectToAction(nameof(Index));
                    }
                }
            }

            // 修正を促す
            ViewData["id_error_message"] = idErrorMessage;
            ViewData["images_error_message"] = imageErrorMessage;
            return View(book);
        }

        // GET: Books/Edit/5
        public async Task<IActionResult> Edit(int? id)
        {
            if (TUser.Power > ACCESSBLE)
                return NeedLogin();

            var book = await context.Book.SingleOrDefaultAsync(m => m.ID == id);
            if (TUser.ID != book.RegisterID) return NotAllowd();
            if (id == null) return NotFound();
            if (book == null) return NotFound();

            return View(book);
        }

        // POST: Books/Edit/5
        // To protect from overposting attacks, please enable the specific properties you want to bind to, for 
        // more details see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, 
            [Bind("ID,RegisterID,IDName,LimitedOffcial,Title,Auther,Contact,PageCount,PublishedDate," +
                "Tags,Sexy,Vaiolence,Grotesque,Description,LastUpdate,CreatedUpdate")]
            Book book)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();

            if (TUser.ID != book.RegisterID) return NotAllowd();
            if (book == null) return NotFound();
            if (id != book.ID) return NotFound();
            
            if (ModelState.IsValid)
            {
                try
                {
                    // エントリーの正規化
                    if (book.Auther == null || book.Auther == "")
                        book.Auther = TUser.Name;
                    else
                        book.Auther = book.Auther.Trim();

                    if (book.Contact == null || book.Contact == "")
                        book.Contact = $"https://twitter.com/{TUser.ScreenName}";
                    else
                        book.Contact = book.Contact.Trim();

                    if (book.PublishedDate == null)
                        book.PublishedDate = DateTime.Now.Date;
                    
                    book.LastUpdate = DateTime.Now;

                    if (book.Tags == null) book.Tags = "";
                    book.Tags = string.Join(",",
                        book.Tags.Split(',').Select(s => s.Trim().Replace(" ", "_")).ToArray());
                    
                    // 更新
                    context.Update(book);
                    await context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!BookExists(book.ID))
                        return NotFound();
                    else
                        throw;
                }
                return RedirectToAction(nameof(Index));
            }
            return View(book);
        }

        // GET: Books/Delete/5
        public async Task<IActionResult> Delete(int? id)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();
            var book = await context.Book.SingleOrDefaultAsync(m => m.ID == id);

            if (TUser.ID != book.RegisterID) return NotAllowd();
            if (id == null) return NotFound();
            if (book == null) return NotFound();

            return View(book);
        }

        // POST: Books/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            if (TUser.Power > ACCESSBLE) return NeedLogin();
            var book = await context.Book.SingleOrDefaultAsync(m => m.ID == id);

            if (TUser.ID != book.RegisterID) return NotAllowd();
            if (book == null) return NotFound();

            context.Book.Remove(book);
            await context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
        }

        private bool BookExists(int id)
        {
            return context.Book.Any(e => e.ID == id);
        }

        private async Task<string> SaveImagesAsync(string id, List<IFormFile> images)
        {
            var dir = "wwwroot" + Path.DirectorySeparatorChar + id;

            if (!Directory.Exists(dir))
                Directory.CreateDirectory(dir);

            if (Directory.GetFiles(dir).Length > 0)
                return "重複があります。IDを変えてください。";

            for (var i = 0; i < images.Count(); i++)
            {
                var image = images[i];
                var path = dir + Path.DirectorySeparatorChar + i + ".jpg";
                try
                {
                    if (image.Length <= 0)
                        throw new Exception($"ファイルの中身がありません。（{id}_{i}）");
                    if (image.Length > 2000000)
                        throw new Exception($"ファイルが2MBを超えています。（{id}_{i}）");

                    using (var stream = new FileStream(path, FileMode.Create))
                        await image.CopyToAsync(stream);

                    using (var img = Image.FromFile(path))
                    using (var bmp = new Bitmap(img))
                    {
                        if (!img.RawFormat.Equals(ImageFormat.Jpeg))
                            throw new Exception($"JPEG画像でアップロードしなおしてみてください。（{id}_{i}）");

                        if (bmp.Width < 300 || bmp.Height < 400)
                            throw new Exception($"画像が小さすぎます。{bmp.Width}x{bmp.Height} （{id}_{i}）");

                        if (bmp.Width > 340 && bmp.Height > 480)
                            Normalize(path);
                    }
                }
                catch (IOException e)
                {
                    Directory.Delete(dir, true);
                    return $"保存できませんでした。管理者にお問い合わせください。（{id}_{i}）";
                }
                catch(OutOfMemoryException e)
                {
                    Directory.Delete(dir, true);
                    return $"画像ファイルではないようです。もしくは破損している可能性があります。（{id}_{i}）";
                }
                catch (Exception e)
                {
                    Directory.Delete(dir, true);
                    return e.Message;
                }
            }
            return "";
        }

        private void Normalize(string path)
        {

        }
    }
}
