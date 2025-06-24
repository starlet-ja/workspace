// 開発環境と本番環境でホストを切り替える
const isLocal = location.hostname === 
  "localhost" || location.hostname.startsWith("192.168.");
const baseUrl = isLocal
  ? "http://192.168.1.17:8080" // 開発環境
  : "https://www.shuurou.com"; // 本番環境

// 村の設定内容をDBに登録する
function setEventData(clickedLngLat) {
	// イベント種別を取得
	const eventType = document.getElementById('event-type').value;
	// イベントタイトルを取得
	const eventTitle = document.getElementById('event-title').value.trim();
	// メッセージを取得
	const message = document.getElementById('message').value.trim();
	// 日付を取得
	const date = document.getElementById('date').value;
	// 開始時刻を取得
	const startTime = document.getElementById('start').value;
	// 募集人数 最小・最大
	const numPeopleMin = document.querySelector('select[name="num-people-min"]').value;
	const numPeopleMax = document.querySelector('select[name="num-people-max"]').value;
	// 参加条件 (checkbox 複数選択)
	const restrictions = [];
	document.querySelectorAll('#check-item-set input[type="checkbox"]:checked').forEach(checkbox => {
	  restrictions.push(checkbox.value);
	});
	// 通信ツール
	let comMethod = null;
	// 招待リンク
	let inviteLink = null;
	// 緯度
	let lat = null;
	// 経度
	let lng = null;
	if (eventType === 'on') {
		// 通信ツール
		comMethod = document.getElementById('com-method').value;
		// 招待リンク
		inviteLink = document.getElementById('invite-link').value;
		// 必須項目のバリデーションチェック
		if (!eventTitle || !date || !startTime || !numPeopleMin || !numPeopleMax || !comMethod) {
		  alert("必須項目を入力して下さい");
		  return;
		}
	} else if (eventType == 'off') {
		lat = clickedLngLat.lat;
		lng = clickedLngLat.lng;
		// 必須項目のバリデーションチェック
		if (!eventTitle || !date || !startTime || !numPeopleMin || !numPeopleMax) {
		  alert("必須項目を入力して下さい");
		  return;
		}
	}
	
	// 入力された日付 + 時刻を Date オブジェクトに変換
	const selectedDateTime = new Date(`${date}T${startTime}`);

	// 現在時刻を取得
	const now = new Date();

	// 比較して過去だったらエラー
	if (selectedDateTime < now) {
	  alert("開始時間が過去です。未来の日時を選択してください。");
	  return;
	}
		
	// データまとめる
	const inpMsg = {
	  eventType: eventType,
	  eventTitle: eventTitle,
	  date: date,
	  startTime: startTime,
	  numPeopleMin: numPeopleMin,
	  numPeopleMax: numPeopleMax,
	  comMethod: comMethod,
	  inviteLink: inviteLink,
	  lat: lat,
	  lng: lng,
	  restrictions: restrictions,
	  message: message
	};
	
	$.ajax({
	  type: "POST",
	  url: `${baseUrl}/set-event`,
	  contentType: "application/json",
	  data: JSON.stringify(inpMsg),
	  dataType: "json"
	}).done(function () {
	  // 成功時 → HTTPステータスが 200 の場合ここが呼ばれる
	  getEventsByDate();
	}).fail(function (jqXHR) {
	  console.log("失敗", jqXHR.status);
	  console.log("失敗", jqXHR.responseJSON);
	  if (jqXHR.responseJSON && jqXHR.responseJSON.error) {
	    alert(jqXHR.responseJSON.error);
	  } else {
	    alert("不明なエラーが発生しました。");
	  }
	});

	// フォームを閉じてリセット
	document.getElementById('comment-form').style.display = 'none';
	document.getElementById('event-title').value = '';
	document.getElementById('message').value = '';
}


  // オンライン画面/オフライン画面の切替
  function setModeCookie(mode, url) {
    document.cookie = "eventMode=" + mode + "; path=/; max-age=86400"; // クッキーに保存
    window.location.href = url; // 指定のURLに遷移
  }
  window.addEventListener('DOMContentLoaded', () => {
    const cookies = document.cookie.split(';').reduce((acc, str) => {
      const [k, v] = str.trim().split('=');
      acc[k] = v;
      return acc;
    }, {});

    const mode = cookies.eventMode;

    if (mode === 'online') {
      document.getElementById('online').classList.add('active');
      document.getElementById('offline').classList.remove('active');
    } else if (mode === 'offline') {
      document.getElementById('offline').classList.add('active');
      document.getElementById('online').classList.remove('active');
    }
  });
  
  // バツボタンの遷移先（トップページ）
	function goTop() {
		const cookies = document.cookie.split(';').reduce((acc, str) => {
		  const [k, v] = str.trim().split('=');
		  acc[k] = v;
		  return acc;
		}, {});
		
		const mode = cookies.eventMode;
		
		if (mode === 'online') {
			window.location.href = "/online-event";
		} else if (mode === 'offline') {
			window.location.href = "/offline-event";
		}
	}
	
  // バツボタンの遷移先（1つ前のページ）
	function goBack() {
	  if (document.referrer) {
	    window.history.back();
	  } else {
	    window.location.href = "/online-event";
	  }
	}

	// 質問ステップの切り替え
	document.addEventListener("DOMContentLoaded", () => {
	  const steps = document.querySelectorAll(".step");
	  let currentStep = 0;

	  function showStep(index) {
	    steps.forEach((step, i) => {
	      step.style.display = i === index ? "block" : "none";
	    });
	    currentStep = index;
	  }

	  // 次へボタン
	  document.querySelectorAll(".next-button").forEach((button, i) => {
	    button.addEventListener("click", () => {
	      if (currentStep + 1 < steps.length) {
	        showStep(currentStep + 1);
	      }
	    });
	  });

	  // 戻るボタン
	  document.querySelectorAll(".prev-button").forEach((button, i) => {
	    button.addEventListener("click", () => {
	      if (currentStep - 1 >= 0) {
	        showStep(currentStep - 1);
	      }
	    });
	  });

	  showStep(currentStep); // 初期表示
	});