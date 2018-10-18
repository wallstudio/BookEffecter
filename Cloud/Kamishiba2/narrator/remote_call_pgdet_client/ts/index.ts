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
}

class CallPgdet{

    public num:number;
    private video: HTMLVideoElement;
    private canvas: HTMLCanvasElement;
    private frameCount = 0;

    constructor(){
        this.num = 1500;
        this.video = <HTMLVideoElement>$("#app>video")[0];
        this.canvas = <HTMLCanvasElement>$("#app>canvas")[0];
        navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || window.navigator.mozGetUserMedia;

        // ここでBindせずに this.frameCallBack を渡してしまうとthisがイベントの発火元になってしまう
        let handle:CallPgdet = this;
        navigator.getUserMedia({video: {width: 360, height: 480, frameRate: 2}, audio: false}, 
            this.frameCallBack.bind(handle),
            console.log
        );
    }

    private frameCallBack(stream: MediaStream){
        this.video.srcObject = stream;
        this.video.play();

        setInterval(()=>{
            console.log(this.frameCount);
            if(this.frameCount++ > 4){
                this.callPgdet(this.video, this.canvas)
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
        formData.append("package", "yukawallstudio.yukamaki");
        formData.append("image", imageFile)

        $.ajax({
            //async: true,
            url: "/narrator/api",
            method: 'post',
            //dataType: 'multipart/form-data',
            data: formData,
            processData: false,
            contentType: false,
        }).done((data, status, xhr) => {
            if(xhr.status == 200){
                const retval = new PgdetRetval(data);
                console.log(`Success! ${JSON.stringify(retval)}`);
            }else{
                console.log(`Success??? Status code: ${xhr.statusCode}`);
            }
        }).fail((xhr, status) => {
            console.log(`Failed! Status code: ${xhr.statusCode}`);
        })
    }
}

window.addEventListener("load",()=>{
    let t = new CallPgdet();
    console.log(t.num);
    console.log("Loaded index.ts");
});