function jbToggleEdit() {
	var editor = $("#custom-source");
	var viewer = $("#custom-body");
	
	if (editor.css("display") == "none") {
		// Currently in view mode, request source and toggle mode.
		var xmlRequest = $.ajax({
			method: "GET",
			url: contextPath + "jb/source/" + sourceURI,
			processData: false
		});
		 
		xmlRequest.done(function(result) {
			editor.prop("value", result);
			$('.custom-editor').toggleClass('custom-hidden'); 
		});
		
		xmlRequest.fail(function(jqXHR, textStatus) {
			alert("Loading source failed: " + jqXHR.statusText + " (" + jqXHR.status + ")");
		});
	} else {
		// Currently in edit mode, save changes.
		var source = editor.prop("value");
		
		var putUri = $("#jb-uri").prop("value");
		
		var xmlRequest = $.ajax({
			method: "PUT",
			url: contextPath + "jb/update/" + putUri,
			processData: false,
			contentType: "text/plain; charset=UTF-8",
			data: source
		});
		 
		xmlRequest.done(function(result) {
			if (putUri == sourceURI) {
				viewer.html(result.body);
				$('#jb-title').text(result.title); 
				$('.custom-editor').toggleClass('custom-hidden'); 
			} else {
				document.location = "/" + result.uri;
			}
		});
		
		xmlRequest.fail(function(jqXHR, textStatus) {
			alert("Saving failed: " + jqXHR.statusText + " (" + jqXHR.status + ")");
		});
	}
	return false;
}