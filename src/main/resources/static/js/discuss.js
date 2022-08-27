$(function (){
    $("#top").click(setTop);
    $("#essence").click(setEssence);
    $("#delete").click(setDelete);

});

function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType, "entityId":entityId, "entityUserId":entityUserId, "postId":postId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code != 0) {
                alert(data.msg);
            } else {
                $(btn).children("i").text(data.likeStatus==1?"已赞":"赞");
                $(btn).children("b").text(data.likeCount);
            }
        }
    );
}

function setTop() {
    $.post(
        CONTEXT_PATH + "/post/top",
        {"postId":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#top").attr("disabled", true);
            } else {
                alert(data.msg)
            }
        }
    );
}

function setEssence() {
    $.post(
        CONTEXT_PATH + "/post/essence",
        {"postId":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#essence").attr("disabled", true);
            } else {
                alert(data.msg)
            }
        }
    );
}

function setDelete() {
    $.post(
        CONTEXT_PATH + "/post/delete",
        {"postId":$("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg)
            }
        }
    );
}