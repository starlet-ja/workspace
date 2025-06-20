// ページ読み込み時にコメントを取得
window.addEventListener('DOMContentLoaded', () => {
  getComments(document.getElementById('comment-list').getAttribute("data-event-id"));
});

// 表示日時のフォーマット
function formatJapaneseDate(isoString) {
	const date = new Date(isoString);
	const month = date.getMonth() + 1;  // 月は0から始まる
	const day = date.getDate();
	const hours = date.getHours().toString().padStart(2, '0');
	const minutes = date.getMinutes().toString().padStart(2, '0');
	return `${month}月${day}日${hours}:${minutes}`;
}

// コメントを取得
function getComments(eventId) {
	$.ajax({
		type: "POST",
		url: `${baseUrl}/get-comments`,
		contentType: "application/json",
		data: JSON.stringify({ eventId: eventId }),
		dataType: "json"
	}).done(function(comments) {
		const commentArea = document.getElementById("comment-list");
		commentArea.innerHTML = ""; // 一度クリア
		
		if (comments.length === 0) {
		  commentArea.innerHTML = "<p>コメントはありません。</p>";
		  return;
		}		

		comments.forEach(comment => {
			const formattedDate = formatJapaneseDate(comment.commentTime);
			const html = `
				<div class="comment">
					<p><strong>${comment.userName}</strong>（${formattedDate}）</p>
					<p>${comment.commentText}</p>
				</div>
			`;
			commentArea.insertAdjacentHTML("beforeend", html);
		});
	}).fail(() => {
		console.log("コメントの取得に失敗しました");
	});
}



// 参加者専用コメントフォーム
const submitBtn = document.getElementById('submitComment');
if (submitBtn) {
	submitBtn.addEventListener('click', (e) => {
	  e.preventDefault(); // フォーム送信を止める（重要）
	  
	  const comment = document.getElementById('comment').value.trim();
	  const eventId = document.getElementById('eventId').value;
	  
	  if (!comment) {
	    alert("コメントを入力してください。");
	    return;
	  }
	  
	  // データまとめる
	  const inpMsg = {
		comment: comment,
		eventId: eventId,
	  };
	
	  $.ajax({
	  	type: "POST",
	  	url: `${baseUrl}/post-comment`,
	  	contentType: "application/json",
	  	data:JSON.stringify(inpMsg),
	  	dataType:"json"
	  }).done(function(){
		// コメントを取得する
		getComments(eventId);
	  }).fail(function(jqXHR) {
	      console.log("失敗", jqXHR.status); // 403など
	      console.log("失敗", jqXHR.responseJSON);
	
	      if (jqXHR.responseJSON && jqXHR.responseJSON.error) {
	          alert(jqXHR.responseJSON.error);
	      } else {
	          alert("不明なエラーが発生しました。");
	      }
	  });
	});
}