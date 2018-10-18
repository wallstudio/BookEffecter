import UUID from "uuid"
import $ from "jquery"


class PgdetRetval{
    public id:string;
    public index: number;
    public score: number;
    public cross: number;

    public constructor(jsonObj: any){
        if("id" in jsonObj && "index" in jsonObj && "score" in jsonObj && "cross" in jsonObj){
            this.id = jsonObj.id;
            this.index = jsonObj.index;
            this.score = jsonObj.score;
            this.cross = jsonObj.cross;
        }else{
            throw `Server internal error! ${jsonObj.toString()}`;
        }
    }

    public past(pack:string, id:HTMLSpanElement, index:HTMLSpanElement,
        score:HTMLSpanElement, cross:HTMLSpanElement, image:HTMLImageElement){
        id.innerHTML = this.id;
        index.innerHTML = this.index.toString();
        score.innerHTML = this.score.toString();
        cross.innerHTML = this.cross.toString();
        image.src = `/static/narrator/packages/${pack}/${("000" + this.index).slice(-3)}.jpg`;
    }
}

class CallPgdet{
    private select: HTMLSelectElement;
    private button: HTMLButtonElement;
    private video: HTMLVideoElement;
    private canvas: HTMLCanvasElement;
    private idLabel: HTMLSpanElement;
    private indexLabel: HTMLSpanElement;
    private scoreLabel: HTMLSpanElement;
    private crossLabel: HTMLSpanElement;
    private image: HTMLImageElement;
    private frameCount = 0;
    private task:NodeJS.Timeout | null = null;
    private isPlay = false;

    constructor(){
        this.select = <HTMLSelectElement>$("#package-list-selection > select")[0]
        this.loadSelect()
        this.button = <HTMLButtonElement>$("#run-button")[0];
        this.button.addEventListener("click", this.runOrStop.bind(this));
        this.video = <HTMLVideoElement>$("#app>div>video")[0];
        this.canvas = <HTMLCanvasElement>$("#app>div>canvas")[0];
        this.idLabel = <HTMLSpanElement>$("#call-id")[0];
        this.indexLabel = <HTMLSpanElement>$("#page-idx")[0];
        this.scoreLabel = <HTMLSpanElement>$("#distance")[0];
        this.crossLabel = <HTMLSpanElement>$("#cross")[0];
        this.image = <HTMLImageElement>$("#app>div>img")[0];
        navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || window.navigator.mozGetUserMedia;

        // ここでBindせずに this.frameCallBack を渡してしまうとthisがイベントの発火元になってしまう
        let handle:CallPgdet = this;
        navigator.getUserMedia({video: {width: 360, height: 480, frameRate: 2}, audio: false}, 
            this.frameCallBack.bind(handle),
            console.log
        );
    }

    public loadSelect(){
        $.ajax({
            url: "/narrator/package-list",
            method: "get",
        }).done((data:any, status, xhr) => {
            if(xhr.status == 200){
                if("packages" in data){
                    const packages:Array<any> = data.packages;

                    for (const packData of packages) {
                        if("package_id" in packData && "package_title" in packData){
                            const option = <HTMLOptionElement>document.createElement("option");
                            option.value = packData.package_id;
                            option.innerHTML = packData.package_title;
                            this.select.appendChild<HTMLOptionElement>(option)
                        }
                    }
                }
            }else{
                console.log(`Success??? Status code: ${xhr.statusCode}`);
            }
        }).fail((xhr, status) => {
            console.log(`Failed! Status code: ${xhr.statusCode}`);
        })
    }

    public runOrStop(){
        this.isPlay = !this.isPlay;
        this.button.innerHTML = this.isPlay ? "STOP" : "START";
    }

    private frameCallBack(stream: MediaStream){
        this.video.srcObject = stream;
        this.video.play();

        this.task = setInterval(()=>{
            console.log(this.frameCount);
            // カメラが慣れるまでスキップ
            if(this.frameCount == 5)
                console.log("Camera maybe became stabled.");
            if(this.frameCount++ >= 5 && this.isPlay){
                this.callPgdet(this.video, this.canvas);
            }
        }, 500);
    }

    private callPgdet(video:HTMLVideoElement, canvas:HTMLCanvasElement){
        const context = <CanvasRenderingContext2D>canvas.getContext("2d");
        context.drawImage(video, 0, 0);

        // IEやSafariが HTMLCanvasElement.toblob() に非対応なので
        const dataurl = canvas.toDataURL('image/jpeg');
        const bin = atob(dataurl.split(',')[1]);
        const buffer = new Uint8Array(bin.length);
        for (let j = 0; j < bin.length; j++) {
            buffer[j] = bin.charCodeAt(j);
        }
        const blob = new Blob([buffer.buffer], { type: 'image/jpeg' });
        const imageFile = <File>blob;

        const formData = new FormData();
        const pack = this.select.options[this.select.selectedIndex].value;
        if(pack == "unselected") 
            return;

        formData.append("package", pack);
        formData.append("image", imageFile)

        $.ajax({
            url: "/narrator/api",
            method: 'post',
            data: formData,
            processData: false,
            contentType: false,
        }).done((data, status, xhr) => {
            if(xhr.status == 200){
                const retval = new PgdetRetval(data);
                retval.past(pack, this.idLabel, this.indexLabel, this.scoreLabel, this.crossLabel, this.image);
                //console.log(`Success! ${JSON.stringify(retval)}`);
            }else{
                console.log(`Success??? Status code: ${xhr.statusCode}`);
            }
        }).fail((xhr, status) => {
            console.log(`Failed! Status code: ${xhr.statusCode}`);
        })
    }
}

window.addEventListener("load",()=>{
    let callPgdet = new CallPgdet();
    console.log("Loaded index.ts");
});