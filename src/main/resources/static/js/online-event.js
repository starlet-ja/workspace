// 制限対応配列
const restrictionLabels = {
  1: "初心者歓迎",
  2: "敬語",
  3: "男性限定",
  4: "女性限定",
  5: "10代OK",
  6: "20代OK",
  7: "30代OK",
  8: "40代OK",
  9: "50代OK",
  10: "60代OK"
};

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

// カレンダーの日付が変更された時にイベントを取得
document.getElementById('startDate').addEventListener('change', getEventsByDate);
document.getElementById('endDate').addEventListener('change', getEventsByDate);

// イベントを取得
function getEventsByDate() {
	// 現在表示されているイベントを全て削除する
	document.getElementById("event-list").innerHTML = "";
	
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
	  data: JSON.stringify({ eventType: "on", startDate: startDate, endDate: endDate }),
	  dataType: "json"
	  }).done(function(events) {
		//取得した要素を表示させるエリア
		const target = document.getElementById("event-list");
		
		if (events.length === 0) {
		  target.innerHTML = `<p>該当するイベントがありませんでした。</p>`;
		  return;
		}
		
	  	  events.forEach(event => {
			//制限を文字列に変換する
			const restrictionText = (event.restrictions || [])
			  .map(r => restrictionLabels[r])
			  .filter(Boolean)
			  .join("、");
			//取得した要素を表示させる
	  		const html = `
	  			<div class="event">
	  				<p>村名:${event.eventTitle}</p>
					<p>主催者:<a href="/user-profile?userId=${event.userId}">${event.host}</a></p>
	  				<p>開催時間:${event.eventDate} ${event.startTime}</p>
	  				<p>募集人数:${event.recruitMin}～${event.recruitMax}</p>
					<p>参加予定人数:${event.participantCount}</p>
					<p>通信手段:${event.comMethod}</p>
	  				<p>参加条件:${restrictionText}</p>
					${event.message ? `<p>主催者コメント:${event.message}</p>` : ''}
					<a href="/event-detail?eventId=${event.eventId}" id="show-details">詳細</a>
	  			</div>
	  		`;
	  		target.insertAdjacentHTML("beforeend", html);
	  	  });
	}).fail(() => {
	  console.log("イベントの取得に失敗しました");
	});	
}

// イベント作成フォームを表示させる
document.getElementById('add-event-button').addEventListener
('click', function() {
	// ログイン状態を取得
	const isLoggedIn = document.getElementById('login-status')?.getAttribute('data-logged-in') === 'true';
	if (!isLoggedIn) {
	  alert('村を作成するには、ログインが必要です');
	  return;
	}
	document.getElementById('comment-form').style.display = 'block';
});

// 村作成ボタンが押された時の処理
document.getElementById('submitComment').addEventListener('click', () => {
  // イベントの情報をDBに登録する
  // イベントの位置情報は無いのでnullとする
  const clickedLngLat = null;
  setEventData(clickedLngLat);
});
