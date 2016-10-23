function jbToggleEdit() {
	var editor = $("#custom-source");
	var viewer = $("#custom-body");
	
	if (editor.css("display") == "none") {
		var url = contextPath + "jb/source/" + sourceuri;
		
		// Currently in view mode, request source and toggle mode.
		var xmlRequest = $.ajax({
			method: "GET",
			url: url,
			processData: false
		});
		 
		xmlRequest.done(function(result) {
			editor.prop("value", result);
			$('.custom-editor').toggleClass('custom-hidden'); 
		});
		
		xmlRequest.fail(function(jqXHR, textStatus) {
			alert("Loading source from '" + url + "' failed: " + jqXHR.statusText + " (" + jqXHR.status + ")");
		});
	} else {
		// Currently in edit mode, save changes.
		var source = editor.prop("value");
		
		var putUri = $("#jb-uri").prop("value");

		var url = contextPath + "jb/update/" + putUri;
		
		var xmlRequest = $.ajax({
			method: "PUT",
			url: url,
			processData: false,
			contentType: "text/plain; charset=UTF-8",
			data: source
		});
		 
		xmlRequest.done(function(result) {
			document.location = "/" + result.uri;
		});
		
		xmlRequest.fail(function(jqXHR, textStatus) {
			alert("Saving page to '" + url + "' failed: " + jqXHR.statusText + " (" + jqXHR.status + ")");
		});
	}
	return false;
}

function jbCreateNew() {
	var editor = $("#custom-source");
	
	if (editor.css("display") != "none") {
		alert("Already editing a page. First save the current edit or cancel.");
		return false;
	}
	
	editor.prop("value", 
		"title=New Page" + "\n" +
		"date=" + "\n" +
		"type=page" + "\n" +
		"tags=" + "\n" +
		"status=published" + "\n" +
		"~~~~~~" + "\n" + 
		"New text." + "\n"
	);

	var uriField = $("#jb-uri");
	var currentPath = sourceuri;
	
	var putUri = uriField.prop("value", currentPath);
	
	$('.custom-editor').toggleClass('custom-hidden'); 
	return false;
}

function jbDeletePage() {
	if (!confirm("You are about to delte the page '" + sourceuri + "'. Are you sure, you want to continue?")) {
		return false;
	}
	
	var url = contextPath + "jb/update/" + sourceuri;
	
	var xmlRequest = $.ajax({
		method: "DELETE",
		url: url,
		processData: false
	});
	
	xmlRequest.done(function(result) {
		document.location = "/" + result.uri;
	});
	
	xmlRequest.fail(function(jqXHR, textStatus) {
		alert("Deleting page '" + url + "' failed: " + jqXHR.statusText + " (" + jqXHR.status + ")");
	});
	
	return false;
}
