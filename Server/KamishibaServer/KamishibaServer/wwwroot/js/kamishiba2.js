window.addEventListener("load", () => {

    // 
    let rowCount = $(".wave-wrap").length;
    let rowHeight = parseInt($(".page-prev-half").css("height"));
    let margin = parseInt($("#audio-drop-zone").css("margin-top"));
    $("#audio-drop-zone").css("height", rowCount * rowHeight - margin * 2);
    
    $("#audio-drop-zone").on("drop", e => {
        e.preventDefault();
        // デザイン
        $(this).css("border", "2px dotted #0B85A1");
        // ロジック
        let files = e.originalEvent.dataTransfer.files;
        if (!files || files.length != 1) {
            alert("読み込めるファイルは1つだけです。")
            return;
        };
        let file = files[0];
        if (!file) return;
        if (file.type != "audio/mpeg" && file.type != "audio/wav") {
            alert("読み込める音声は MP3 または WAV だけです。もしくはファイルが破損している可能性があります。")
            return;
        }

        $("#audio-drop-zone").css("display", "none");
        $(".first-wave-cell .select-span").css("display", "block");
        $(".first-wave-cell .waveform").css("display", "block");
        $(".other-wave-cell").css("display", "block");
        $(".first-wave-cell")[0].removeAttribute("rowspan");

        let blobUrl = window.URL.createObjectURL(file);

        let wavesurfer = WaveSurfer.create({
            container: "#waveform-0",
            waveColor: "skyblue",
            height: parseInt($(".page-prev-half").css("height")),
            progressColor: "blue",
            interact: false,
            barWidth: 2,
            cursorWidth: 1,
            hideScrollbar: true,
            normalize: true,
            responsive: true,
            plugins: [
                WaveSurfer.timeline.create({
                    container: "#wave-timeline1"
                })
            ]
        });

        // 各行にコピー
        let graphicCopy = () => {
            let originHtml = $("#waveform-0")[0];
            let originBgCanvas = $("#waveform-0 > wave > canvas")[0];
            let originFgCanvas = $("#waveform-0 > wave > wave > canvas")[0];
            $(".waveform").each((i, e) => {
                if (i > 0) {
                    let html = $("#waveform-" + i)[0];
                    html.innerHTML = originHtml.innerHTML;
                    let bgCanvas = $("#waveform-" + i + " > wave > canvas")[0];
                    let fgCanvas = $("#waveform-" + i + " > wave > wave > canvas")[0];
                    let bgData = originBgCanvas.getContext("2d")
                        .getImageData(0, 0, originBgCanvas.width, originBgCanvas.height);
                    let fgData = originFgCanvas.getContext("2d")
                        .getImageData(0, 0, originFgCanvas.width, originFgCanvas.height);
                    bgCanvas.getContext("2d").putImageData(bgData, 0, 0);
                    fgCanvas.getContext("2d").putImageData(fgData, 0, 0);
                }
            });

            let originalTLHtml = $("#wave-timeline1")[0];
            let tLHtml = $("#wave-timeline2")[0];
            tLHtml.innerHTML = originalTLHtml.innerHTML;
            let originalTLCanvas = $("#wave-timeline1 canvas")[0];
            let tLCanvas = $("#wave-timeline2 canvas")[0];
            let tlGraphic = originalTLCanvas.getContext("2d")
                .getImageData(0, 0, originalTLCanvas.width, originalTLCanvas.height);;
            tLCanvas.getContext("2d").putImageData(tlGraphic, 0, 0);
        };
        let stateCopy = () => {
            let originalStyle = $("#waveform-0 > wave > wave");
            $(".waveform").each((i, e) => {
                if (i > 0) {
                    let style = $("#waveform-" + i + " > wave > wave");
                    style.css("width", originalStyle.css("width"));
                }
            });
        };
        
        // ReadyしてもすぐにCanvasが描画されない？
        let isGraphicInited = -5;
        wavesurfer.on("ready", () => {
            graphicCopy();
            wavesurfer.play();
        });
        wavesurfer.on("audioprocess", () => {
            if (isGraphicInited < 0) {
                graphicCopy();
                isGraphicInited ++;
            }
            stateCopy();
        });
        wavesurfer.on("seek", stateCopy);

        // 選択領域のキャンバスの初期化
        $(".select-span canvas").each((i, e) => {
            e.setAttribute("width", $(e).css("width"));
            e.setAttribute("height", $(e).css("height"));
        });

        // コピーのシーク操作戻し
        let clickStatus = [];
        let dragStatus = [];
        let startEnd = [];
        $(".waveform").each((i, e) => {
            clickStatus.push(false);
            dragStatus.push(false);
            startEnd.push([0, 0]);
            let selectSpan = $("#select-span-" + i + " canvas")[0];
            let selectSpanContext = selectSpan.getContext("2d");
            selectSpanContext.globalAlpha = 0.4;
            let x;
            $(e).mousedown(me => {
                me.offsetX += 3;
                x = me.offsetX;
                clickStatus[i] = true;
            }).mouseup(me => {
                me.offsetX += 3;
                let delta = x - me.offsetX;
                let seek = x / $(e).find("wave > canvas")[0].width;
                if (clickStatus && Math.abs(delta) <= 2) {
                    wavesurfer.seekTo(seek);
                    wavesurfer.play();
                } else if (dragStatus[i]) {
                    let p0 = x / $(e).find("wave > canvas")[0].width * wavesurfer.getDuration();
                    let p1 = me.offsetX / $(e).find("wave > canvas")[0].width * wavesurfer.getDuration();
                    wavesurfer.play(Math.min(p0, p1), Math.max(p0, p1));
                }
                clickStatus.fill(false);
                dragStatus.fill(false);
            }).mouseleave(me => {
                me.offsetX += 3;
                if (dragStatus[i]) {
                    let p0 = x / $(e).find("wave > canvas")[0].width * wavesurfer.getDuration();
                    let p1 = me.offsetX / $(e).find("wave > canvas")[0].width * wavesurfer.getDuration();
                    wavesurfer.play(Math.min(p0, p1), Math.max(p0, p1));
                }
                clickStatus.fill(false);
                dragStatus.fill(false);
            }).mousemove(me => {
                me.offsetX += 3;
                if (!clickStatus[i]) return;
                let delta = x - me.offsetX;
                if (Math.abs(delta) > 2) {
                    dragStatus.fill(false);
                    dragStatus[i] = true;
                    selectSpanContext.clearRect(0, 0, selectSpan.width, selectSpan.height);
                    selectSpanContext.fillStyle = "rgb(64, 255, 128)";
                    selectSpanContext.fillRect(x, 0, -delta, selectSpan.height);

                    let p0 = x / $(e).find("wave > canvas")[0].width * wavesurfer.getDuration();
                    let p1 = me.offsetX / $(e).find("wave > canvas")[0].width * wavesurfer.getDuration();
                    startEnd[i] = [Math.min(p0, p1).toFixed(1), Math.max(p0, p1).toFixed(1)];
                    $("#TrackTiming")[0].value =
                        "[" + startEnd.map(de => de.join(", ")).join(", ") + "]";
                }
            });
        });

        $(e.target).css("display", "none");
        //$("#waveform").css("display", "block");

        wavesurfer.load(blobUrl);
        console.log("Kamishiba: Loaded audio & converted " + files.length)
    })

    $("#audio-drop-zone").on('dragenter', e => {
        e.stopPropagation();
        e.preventDefault();
        $(this).css('border', '2px solid #0B85A1');
    });

    $("#audio-drop-zone").on('dragover', e => {
        e.stopPropagation();
        e.preventDefault();
    });

    $(document).on('drop', e => {
        e.stopPropagation();
        e.preventDefault();
    });
    console.log("Kamishiba: Drop zone was initialized.");
});