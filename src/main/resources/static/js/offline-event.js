// クッキーにオン/オフ情報が無い場合は、オフにする
window.addEventListener('DOMContentLoaded', () => {
  const cookies = document.cookie.split(';').reduce((acc, str) => {
    const [k, v] = str.trim().split('=');
    acc[k] = v;
    return acc;
  }, {});

  if (!cookies.eventMode) {
    document.getElementById('offline').classList.add('active');
    document.getElementById('online').classList.remove('active');
  }
});

// マップ上の既存ポップアップ全削除用に配列管理
let currentPopups = [];

  // カレンダーに本日の日付を入れる
  const today = getDateAfterNDays(0);
  const after30DaysStr = getDateAfterNDays(30);
  document.getElementById('startDate').value = today;
  document.getElementById('endDate').value = after30DaysStr;
  // 日付に該当するイベントを取得する
  getEventsByDate();

/* カレンダーの設定 */
// N日後の日付を取得
function getDateAfterNDays(n) {
  const today = new Date();
  today.setDate(today.getDate() + n); // N日後に進める
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// ログイン状態の確認
const isLoggedIn = document.getElementById('login-status').getAttribute('data-logged-in') === 'true';

// オンオフボタン切替
document.getElementById('online').addEventListener('click', function () {
  document.getElementById('offline').classList.add('active');
  document.getElementById('online').classList.remove('active');
});

document.getElementById('offline').addEventListener('click', function () {
  document.getElementById('online').classList.add('active');
  document.getElementById('offline').classList.remove('active');
});

// マップ上のポップアップを全て削除する
function removeAllPopups() {
  currentPopups.forEach(popup => popup.remove());
  currentPopups = [];
}

// カレンダーの日付が変更された時にイベントを取得
document.getElementById('startDate').addEventListener('change', getEventsByDate);
document.getElementById('endDate').addEventListener('change', getEventsByDate);

// カレンダー日付のイベント一覧をマップに追加
function getEventsByDate() {
  // マップ上の既存のポップアップを全て削除する
  removeAllPopups();

  // カレンダーに表示されている日付を取得
  const startDate = document.getElementById('startDate').value;
  const endDate = document.getElementById('endDate').value;

  if (!startDate || !endDate) {
    alert("開始日と終了日を両方選択してください。");
    return;
  }

  if (startDate > endDate) {
    alert("開始日は終了日より前としてください。");
    return;
  }

  $.ajax({
    type: "POST",
    url: `${baseUrl}/get-events`,
    contentType: "application/json",
    data: JSON.stringify({ eventType: "off", startDate: startDate, endDate: endDate }),
    dataType: "json"
  }).done(function (events) {
    events.forEach(event => {
      // 取得した要素を表示させる
      const popupHTML = `
        <div class="popup-contents">
          村名：${event.eventTitle}<br/>
          集合：${event.eventDate} ${event.startTime}<br/>
          参加予定：${event.participantCount}人<br/>
          <a href="/event-detail?eventId=${event.eventId}" id="show-details">詳細</a>
        </div>
      `;
      const popup = new maplibregl.Popup({ closeButton: false, closeOnClick: false, anchor: 'bottom' })
        .setLngLat([event.lng, event.lat])
        .setHTML(popupHTML)
        .addTo(map);
      currentPopups.push(popup);
    });
  }).fail(() => {
    console.log("イベントの取得に失敗しました");
  });
}

// ページの読み込み時に位置を復元
const saved = localStorage.getItem("mapState");
let center = [139.75, 35.68]; // デフォルト座標（東京）
let zoom = 10; // デフォルトズーム

if (saved) {
  try {
    const parsed = JSON.parse(saved);
    center = [parsed.lng, parsed.lat];
    zoom = parsed.zoom;
  } catch (e) {
    console.warn("地図状態の復元に失敗:", e);
  }
}

// マップを作成する
const map = new maplibregl.Map({
  container: 'map',
  style: 'https://tile.openstreetmap.jp/styles/osm-bright-ja/style.json',
//  style: '/style/horror-style.json',
  center: center,
  zoom: zoom,
  pitchWithRotate: false,
  dragRotate: false
});

// マップの初期表示
map.on('load', () => {
  map.touchZoomRotate.disableRotation();
  map.dragRotate.disable();

  // レイヤーの一覧からテキスト表示のレイヤーを探して変更
  const layers = map.getStyle().layers;
  layers.forEach(layer => {
    if (layer.type === 'symbol' && layer.layout && layer.layout['text-size']) {
      // すべてのラベルサイズを大きく（例：現在より +8）
      const newSize = typeof layer.layout['text-size'] === 'number'
        ? layer.layout['text-size'] + 8
        : 20;
      map.setLayoutProperty(layer.id, 'text-size', newSize);
    }
  });
});

// 遷移前にマップの位置をlocalStorageに保存
map.on('moveend', () => {
  const center = map.getCenter(); // 緯度経度
  const zoom = map.getZoom();

  localStorage.setItem("mapState", JSON.stringify({
    lat: center.lat,
    lng: center.lng,
    zoom: zoom
  }));
});

// クリックされた座標
let clickedLngLat = null;
// ピンの登録
let currentMarker = null;

// マップがクリックされた時の処理
map.on('click', (e) => {
  // クリック元が「.show-details-link」の場合はスキップ
  if (e.originalEvent.target.closest('.show-details-link')) {
    return; // 詳細ボタンは普通にタップ可能にする
  }

  // ログインしていない場合は投稿させない
  if (!isLoggedIn) {
    alert("コメント投稿はログインが必要です。ログインしてください。");
    return;
  }
  
  // 確認ダイアログ
  const ok = confirm("ここに村を作りますか？");
  if (!ok) {
    return; // 「いいえ」を選んだら何もしない
  }

  // 以前のピンがあれば削除
  if (currentMarker) {
    currentMarker.remove();
  }

  // 座標を保存
  clickedLngLat = e.lngLat;

  // ピンをマップ上に追加
  currentMarker = new maplibregl.Marker()
    .setLngLat(clickedLngLat)
    .addTo(map);

  const form = document.getElementById('comment-form');
  form.style.display = 'block';
});

// 村作成ボタンが押された時の処理
document.getElementById('submitComment').addEventListener('click', () => {
  // 以前のピンがあれば削除
  if (currentMarker) {
    currentMarker.remove();
  }
  // イベントの情報をDBに登録する
  setEventData(clickedLngLat);
});

// フォームから入力された情報をDBに登録
function posMsg(inpMsg) {
  $.ajax({
    type: "POST",
    url: `${baseUrl}/post-message`,
    contentType: "application/json",
    data: JSON.stringify(inpMsg),
    dataType: "json"
  }).done(function (data) {
    // 成功時の処理
    id_in_area = data;
    console.log(data);
  }).fail(function () {
    // 失敗時の処理
    console.log("失敗");
  }).always(function () {
    // 成功、失敗にかかわらず実行する処理
  });
}