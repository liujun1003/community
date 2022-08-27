$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送Ajax请求
	$.post(
		CONTEXT_PATH + "/post/add",
		{"title": title, "content": content},
		function (data) {
			data = $.parseJSON(data);
			$("#hintModalLabel").text(data.msg);
			$("#hintModal").modal("show");

			setTimeout(function(){
				if (data.code === 0) {
					window.location.reload();
				}
				$("#hintModal").modal("hide");
			}, 2000);
		}
	)
}