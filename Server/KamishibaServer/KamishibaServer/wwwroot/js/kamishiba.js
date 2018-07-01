var GlobakImages = {};

window.addEventListener("load", () => {

    // Drop-zone
    const MAX_WIDTH = 340;
    const MAX_HEIGHT = 480;
    
    $("#drop-zone").on("drop", e => {
        e.preventDefault();
        // デザイン
        $(this).css("border", "2px dotted #0B85A1");
        // ロジック
        let files = e.originalEvent.dataTransfer.files;
        if (files.length > 30) {
            alert("一度に読み込める画像は30舞までです。")
            return;
        };
        for (let i = 0; i < files.length; i++) {
            if (files[i].type != 'image/jpeg' && files[i].type != 'image/png') {
                alert("読み込める画像はJEPGまたはPNGだけです。もしくはファイルが破損している可能性があります。")
                return;
            }
        }

        for (let i = 0; i < files.length; i++) {
            let file = files[i];
            if (!file) return;
            if (file.type != 'image/jpeg' && file.type != 'image/png') return;

            let canvas = $("<canvas>")[0];
            let image = new Image();
            let reader = new FileReader();

            reader.onload = f => {
                image.onload = () => {
                    let w, h;
                    if (image.width > image.height) {
                        let aspect = image.height / image.width;
                        w = MAX_WIDTH;
                        h = MAX_WIDTH * aspect;
                    } else {
                        let aspect = image.width / image.height;
                        w = MAX_HEIGHT * aspect;
                        h = MAX_HEIGHT;
                    }

                    canvas.width = w;
                    canvas.height = h;
                    canvas.getContext("2d").drawImage(image, 0, 0, image.width, image.height, 0, 0, w, h);
                    let dataurl = canvas.toDataURL('image/jpeg');
                    let bin = atob(dataurl.split(',')[1]);
                    let buffer = new Uint8Array(bin.length);
                    for (let j = 0; j < bin.length; j++) {
                        buffer[j] = bin.charCodeAt(j);
                    }
                    let blob = new Blob([buffer.buffer], { type: 'image/jpeg' });
                    file = blob;

                    let id = (("00000000" + Math.floor(Math.abs(Math.random() * (1 << 31))).toString(16)).slice(-8)
                        + ("00000000" + Math.floor(Math.abs(Math.random() * (1 << 31))).toString(16)).slice(-8)).toUpperCase();
                    GlobakImages[id] = file;
                    let div = $("<div>");
                    div.attr("draggable", "true");
                    div.addClass("page-prev");
                    div.hover(e => {
                        $(e.target).find(".cancel").css("display", "block");
                    }, e => {
                        $(e.target).find(".cancel").css("display", "none");
                    });
                    let img = $("<img>");
                    img.addClass("page-prev-image id-is-" + id);
                    img.attr("src", window.URL.createObjectURL(file));
                    img.attr("draggable", "false");
                    img.hover(e => {
                        $(e.target).parent().find(".cancel").css("display", "block");
                    }, e => {
                        $(e.target).parent().find(".cancel").css("display", "none");
                    });
                    let cancel = $("<img>");
                    cancel.attr("src", "/images/cancel.png");
                    cancel.addClass("cancel");
                    cancel.on("click", e => {
                        let removeImage = $(e.target).parent().find(".page-prev-image")[0];
                        let imageId = removeImage.getAttribute("class").match(/id-is-([0-9a-fA-F]+)/)[1];
                        window.URL.revokeObjectURL(removeImage.getAttribute("src"));
                        $(e.target).parent().remove();
                        delete GlobakImages[id];
                    });
                    cancel.hover(e => {
                        $(e.target).css("display", "block");
                    }, e => {
                        $(e.target).css("display", "none");
                    });
                    $("#images-list").append(div.append(img).append(cancel));
                }
                image.src = f.target.result;
            }
            reader.readAsDataURL(file);

            console.log("Kamishiba: Loaded image & resized " + (i+1) + "/" + files.length)
        }

        Sortable.create($("#images-list")[0], { animation: 100 });
    })

    $("#drop-zone").on('dragenter', e => {
        e.stopPropagation();
        e.preventDefault();
        $(this).css('border', '2px solid #0B85A1');
    });

    $("#drop-zone").on('dragover', e => {
        e.stopPropagation();
        e.preventDefault();
    });

    $(document).on('drop', e => {
        e.stopPropagation();
        e.preventDefault();
    });

    // Agree
    $(".check-wrap span").on("click", e => {
        $("#is-agreed")[0].checked = !$("#is-agreed")[0].checked;
    });

    // Altanative button 
    $("#open-alt-btn")[0].innerHTML
        = $(".book-images-content .text-list")[0].innerHTML.split(",")[0];
    $("#open-alt-btn").on("click", e => {
        let btn = $(e.target)[0];
        let drop = $("#drop-zone");
        let alt = $("#alt-image-upload");
        let isOpenedAltOld = alt.css("display") != "none";
        let isOpenedAltNew = !isOpenedAltOld;
        let textList = $(".book-images-content .text-list")[0].innerHTML.split(",");

        if (isOpenedAltNew) {
            alt.css("display", "block");
            drop.css("opacity", "0.2");
            drop.css("pointer-events", "none");
            btn.innerHTML = textList[1];
        } else {
            alt.css("display", "none");
            drop.css("opacity", "1");
            drop.css("pointer-events", "auto");
            btn.innerHTML = textList[0];
        }
    });

    $("#alt-image-upload input").change(() => {
        let output = $("#alt-images-list");
        let input = $("#alt-image-upload input");
        output.empty();
        for (let i = 0; i < input[0].files.length; i++) {
            let li = $("<li>");
            li.text(input[0].files[i].name);
            output.append(li);
        }
    });

    // Send Button
    $(".book-conform .btn").on("click", e => {

        if (!$("#is-agreed")[0].checked) {
            alert("同意するにチェックを入れてください。");
            return;
        }

        let alt = $("#alt-image-upload");
        let isOpenedAlt = alt.css("display") != "none";

        if (!isOpenedAlt) {
            let formData = new FormData($("#book-upload-from")[0]);
            $("#images-list .page-prev-image").each((i, e) => {
                let imageId = e.getAttribute("class").match(/id-is-([0-9a-fA-F]+)/)[1];
                let imageBlob = GlobakImages[imageId];
                console.log("Kamishiba: [" + i + "]: " + imageId);
                console.log(imageBlob);
                formData.append("Images", imageBlob, i + "_" + imageId + ".jpg");
            });

            $.ajax({
                url: $("#book-upload-from")[0].getAttribute("action"),
                method: 'post',
                dataType: 'html',
                data: formData,
                processData: false,
                contentType: false
            }).done(res => {
                console.log('Kamishiba: AJAX SUCCESS');
                let meta = $(res).find(".book-meta-content")[0];
                let img = $(res).find(".book-images-content .field-validation-error")[0];

                if (!meta || !img) {
                    $("html")[0].innerHTML = res;
                    history.pushState('', '', '/Books/Index');
                }

                let preMeta = $(".book-meta-content")[0];
                let preImg = $(".book-images-content .field-validation-error")[0];
                preMeta.innerHTML = meta.innerHTML;
                preImg.innerHTML = img.innerHTML;
                scrollTo(0, 0);
            }).fail((jqXHR, textStatus, errorThrown) => {
                console.log('Kamishiba: AJAX ERROR', jqXHR, textStatus, errorThrown);
            });
        } else {
            $("#book-upload-from")[0].submit();
        }
    });

    console.log("Kamishiba: Drop zone was initialized.");
});