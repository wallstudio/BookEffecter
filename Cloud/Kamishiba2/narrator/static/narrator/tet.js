function Tet(){
  var COLS = 10, ROWS = 10;  // 盤面のマスの数
  var board = [];  // 盤面の状態を保持する変数
  var lose;  // 一番うえまで積み重なっちゃったフラグ
  var interval;  // ゲームタイマー保持用変数
  var current; // 現在操作しているブロック
  var currentX, currentY; // 現在操作しているブロックのいち
  // ブロックのパターン
  var shapes = [
    [ 1, 1, 1, 1 ],
    [ 1, 1, 1, 1 ],
    [ 1, 1, 1, 1 ],
    [ 1, 1, 0, 0,
      1, 1 ],
    [ 1, 1, 0, 0,
      0, 1, 1 ],
    [ 0, 1, 1, 0,
      1, 1 ],
    [ 0, 1, 0, 0,
      1, 1, 1 ]
  ];
  // ブロックの色
  var colors = [
    'cyan', 'orange', 'blue', 'yellow', 'red', 'green', 'purple'
  ];

  // shapesからランダムにブロックのパターンを出力し、盤面の一番上へセットする
  function newShape() {
    var id = Math.floor( Math.random() * shapes.length );  // ランダムにインデックスを出す
    var shape = shapes[ id ];
    // パターンを操作ブロックへセットする
    current = [];
    for ( var y = 0; y < 4; ++y ) {
      current[ y ] = [];
      for ( var x = 0; x < 4; ++x ) {
        var i = 4 * y + x;
        if ( typeof shape[ i ] != 'undefined' && shape[ i ] ) {
          current[ y ][ x ] = id + 1;
        }
        else {
          current[ y ][ x ] = 0;
        }
      }
    }
    // ブロックを盤面の上のほうにセットする
    currentX = 5;
    currentY = 0;
  }

  // 盤面を空にする
  function init() {
    for ( var y = 0; y < ROWS; ++y ) {
      board[ y ] = [];
      for ( var x = 0; x < COLS; ++x ) {
        board[ y ][ x ] = 0;
      }
    }
  }

  // newGameで指定した秒数毎に呼び出される関数。
  // 操作ブロックを下の方へ動かし、
  // 操作ブロックが着地したら消去処理、ゲームオーバー判定を行う
  function tick() {
    // １つ下へ移動する
    if ( valid( 0, 1 ) ) {
      ++currentY;
    }
    // もし着地していたら(１つしたにブロックがあったら)
    else {
      freeze();  // 操作ブロックを盤面へ固定する
      clearLines();  // ライン消去処理
      if (lose) {
        // もしゲームオーバなら最初から始める
        newGame();
        return false;
      }
      // 新しい操作ブロックをセットする
      newShape();
    }
  }

  // 操作ブロックを盤面にセットする関数
  function freeze() {
    for ( var y = 0; y < 4; ++y ) {
      for ( var x = 0; x < 4; ++x ) {
        if ( current[ y ][ x ] ) {
          board[ y + currentY ][ x + currentX ] = current[ y ][ x ];
        }
      }
    }
  }

  // 操作ブロックを回す処理
  function rotate( current ) {
    var newCurrent = [];
    for ( var y = 0; y < 4; ++y ) {
      newCurrent[ y ] = [];
      for ( var x = 0; x < 4; ++x ) {
        newCurrent[ y ][ x ] = current[ 3 - x ][ y ];
      }
    }
    return newCurrent;
  }

  // 一行が揃っているか調べ、揃っていたらそれらを消す
  function clearLines() {
    for ( var y = ROWS - 1; y >= 0; --y ) {
      var rowFilled = true;
      // 一行が揃っているか調べる
      for ( var x = 0; x < COLS; ++x ) {
        if ( board[ y ][ x ] == 0 ) {
          rowFilled = false;
          break;
        }
      }
      // もし一行揃っていたら, サウンドを鳴らしてそれらを消す。
      if ( rowFilled ) {
        document.getElementById( 'clearsound' ).play();  // 消滅サウンドを鳴らす
        // その上にあったブロックを一つずつ落としていく
        for ( var yy = y; yy > 0; --yy ) {
          for ( var x = 0; x < COLS; ++x ) {
            board[ yy ][ x ] = board[ yy - 1 ][ x ];
          }
        }
        ++y;  // 一行落としたのでチェック処理を一つ下へ送る
      }
    }
  }


  // キーボードが押された時に呼び出される関数
  function keyPress( key ) {
    switch ( key ) {
    case 'left':
      if ( valid( -1 ) ) {
        --currentX;  // 左に一つずらす
      }
      break;
    case 'right':
      if ( valid( 1 ) ) {
        ++currentX;  // 右に一つずらす
      }
      break;
    case 'down':
      if ( valid( 0, 1 ) ) {
        ++currentY;  // 下に一つずらす
      }
      break;
    case 'rotate':
      // 操作ブロックを回す
      var rotated = rotate( current );
      if ( valid( 0, 0, rotated ) ) {
        current = rotated;  // 回せる場合は回したあとの状態に操作ブロックをセットする
      }
      break;
    }
  }

  // 指定された方向に、操作ブロックを動かせるかどうかチェックする
  // ゲームオーバー判定もここで行う
  function valid( offsetX, offsetY, newCurrent ) {
    offsetX = offsetX || 0;
    offsetY = offsetY || 0;
    offsetX = currentX + offsetX;
    offsetY = currentY + offsetY;
    newCurrent = newCurrent || current;
    for ( var y = 0; y < 4; ++y ) {
      for ( var x = 0; x < 4; ++x ) {
        if ( newCurrent[ y ][ x ] ) {
          if ( typeof board[ y + offsetY ] == 'undefined'
              || typeof board[ y + offsetY ][ x + offsetX ] == 'undefined'
              || board[ y + offsetY ][ x + offsetX ]
              || x + offsetX < 0
              || y + offsetY >= ROWS
              || x + offsetX >= COLS ) {
                if (offsetY == 1 && offsetX-currentX == 0 && offsetY-currentY == 1){
                  console.log('game over');
                  document.getElementById( 'gameover-sound' ).play();
                  lose = true; // もし操作ブロックが盤面の上にあったらゲームオーバーにする
                }
                return false;
              }
        }
      }
    }
    return true;
  }

  function newGame() {
    clearInterval(interval);  // ゲームタイマーをクリア
    init();  // 盤面をまっさらにする
    newShape();  // 新しい
    lose = false;
    interval = setInterval( tick, 250 );  // 250ミリ秒ごとにtickという関数を呼び出す
  }

  newGame(); // ゲームを開始する

  /*
  キーボードを入力した時に一番最初に呼び出される処理
  */
  document.body.onkeydown = function( e ) {
    // キーに名前をセットする
    var keys = {
      37: 'left',
      39: 'right',
      40: 'down',
      38: 'rotate'
    };

    if ( typeof keys[ e.keyCode ] != 'undefined' ) {
      // セットされたキーの場合はtetris.jsに記述された処理を呼び出す
      keyPress( keys[ e.keyCode ] );
      // 描画処理を行う
      render();
    }
  };
  document.getElementById("virtual-button-left").addEventListener("click", ()=>{
    keyPress("left");
  });
  document.getElementById("virtual-button-right").addEventListener("click", ()=>{
    keyPress("right");
  });

  /*
  現在の盤面の状態を描画する処理
  */
  var canvas = document.getElementsByTagName( 'canvas' )[ 0 ];  // キャンバス
  var ctx = canvas.getContext( '2d' ); // コンテクスト
  var W = 360, H = 360;  // キャンバスのサイズ
  var BLOCK_W = W / COLS, BLOCK_H = H / ROWS;  // マスの幅を設定

  // x, yの部分へマスを描画する処理
  function drawBlock( x, y ) {
    ctx.fillRect( BLOCK_W * x, BLOCK_H * y, BLOCK_W - 1 , BLOCK_H - 1 );
    ctx.strokeRect( BLOCK_W * x, BLOCK_H * y, BLOCK_W - 1 , BLOCK_H - 1 );
  }

  // 盤面と操作ブロックを描画する
  function render() {
    ctx.clearRect( 0, 0, W, H );  // 一度キャンバスを真っさらにする
    img = document.getElementById("hasi-image");
    ctx.drawImage(img, 0, 0, 360, 360);
    ctx.strokeStyle = 'black';  // えんぴつの色を黒にする

    let tmp = [];
    for ( var y = 0; y < ROWS; ++y ) {
      let ttmp = [];
      for ( var x = 0; x < COLS; ++x ) {
        ttmp.push(true);
      }
      tmp.push(ttmp);
    }

    // 盤面を描画する
    for ( var x = 0; x < COLS; ++x ) {
      for ( var y = 0; y < ROWS; ++y ) {
        if ( board[ y ][ x ]) {  // マスが空、つまり0ゃなかったら
          tmp[y][x] = false;
        }
      }
    }

    // 操作ブロックを描画する
    for ( var y = 0; y < 4; ++y ) {
      for ( var x = 0; x < 4; ++x ) {
        if ( current[ y ][ x ] ) {
          tmp[currentY + y][currentX + x] = false;
        }
      }
    }

    for ( var x = 0; x < COLS; ++x ) {
      for ( var y = 0; y < ROWS; ++y ) {
        if(tmp[y][x]){
          ctx.fillStyle = "white";
          drawBlock( x,y );  // マスを描画
        }
      }
    }
  }

  // 30ミリ秒ごとに状態を描画する関数を呼び出す
  setInterval( render, 30 );
}

window.addEventListener("load",()=>{
  const launcher = document.getElementById("launch-minigame");
  launcher.addEventListener("click", ()=>{
      const tet = new Tet();
      
      const video = document.getElementsByTagName("video")[0];
      video.remove();
      const canvas = document.getElementsByTagName("canvas")[0];
      canvas.style.display = "block";
      canvas.height = 360;
      const image = document.getElementById("result-image");
      image.remove();


  });
});