
window.addEventListener("load", () => {

    $(".first-wave-cell .select-span").css("display", "block");
    $(".first-wave-cell .waveform").css("display", "block");
    $(".other-wave-cell").css("display", "block");
    $(".first-wave-cell")[0].removeAttribute("rowspan");

    let blobUrl = $("#waveform-0")[0].getAttribute("data").split(";")[0];

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
        closeAudioContext: true,
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

        ig = wavesurfer.exportImage();
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
    let graphicCopyLoop;
    wavesurfer.on("ready", () => {
        graphicCopy();
        refrectFromData($("#waveform-0")[0].getAttribute("data").split(";")[1]);
        // 力業💛
        graphicCopyLoop = setInterval(graphicCopy, 500);
    });
    wavesurfer.on("audioprocess", stateCopy);
    wavesurfer.on("seek", stateCopy);

    // UIの設定
    let clickStatus = [];
    let dragStatus = [];
    let startEnd = [];
    let resizeTask;
    let configSelescSpan = () => {
        // 選択領域のキャンバスの初期化
        $(".select-span canvas").each((i, e) => {
            e.setAttribute("width", $(e).css("width"));
            e.setAttribute("height", $(e).css("height"));
        });

        // コピーのシーク操作戻し
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
                }
                clickStatus.fill(false);
                dragStatus.fill(false);
            }).mouseleave(me => {
                clickStatus.fill(false);
                dragStatus.fill(false);
            });
        });
    }
    let unConfigSelectSpan = () => {
        $(".waveform").each((i, e) => {
            $(e).unbind("mousedown").unbind("mouseup")
                .unbind("mouseleave").unbind("mousemove");
        });
    };
    let refrectFromData = (dataStr) => {
        let dataSeq = dataStr.replace(/[\[\]]/, "").split(",");
        let data = [];
        for (let i = 0; i < Math.floor(dataStr.length / 2); i++) {
            data[i] = [parseFloat(dataSeq[i * 2]), parseFloat(dataSeq[i * 2 + 1])];
        }
        $(".waveform").each((i, e) => {
            if (i >= data.length) return;
            let start = data[i][0];
            let end = data[i][1];
            startEnd[i] = [start, end];
            let selectSpan = $("#select-span-" + i + " canvas")[0];
            let selectSpanContext = selectSpan.getContext("2d");
            let a = start * $(e).find("wave > canvas")[0].width / wavesurfer.getDuration();
            let b = end * $(e).find("wave > canvas")[0].width / wavesurfer.getDuration();
            selectSpanContext.globalAlpha = 0.4;
            selectSpanContext.clearRect(0, 0, selectSpan.width, selectSpan.height);
            selectSpanContext.fillStyle = "rgb(64, 255, 128)";
            selectSpanContext.fillRect(a, 0, b - a, selectSpan.height);
        });
    };
    unConfigSelectSpan();
    configSelescSpan();

    // サムネを押すと再生
    $(".img-column").each((i, e) => {
        $(e).on("click", () => wavesurfer.play(startEnd[i][0], startEnd[i][1]));
    });
    $(".count-column").each((i, e) => {
        $(e).on("click", () => wavesurfer.play(startEnd[i][0], startEnd[i][1]));
    });

    // ページを閉じてもオーディオが解放されないときがある（高負荷時？）
    // 本来解放されるはずなので…気休めに
    $(window).on("beforeunload", () => {
        clearInterval(graphicCopyLoop);
        wavesurfer.stop();
        wavesurfer.destroy();
        console.log("Kamishiba: Destroy audio.");
        return;
    });

    wavesurfer.load(blobUrl);

    console.log("Kamishiba: Audio player was initialized.");
});