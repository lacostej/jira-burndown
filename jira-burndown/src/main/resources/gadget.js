if(!String.prototype.startsWith){
  String.prototype.startsWith = function (str) {
    return !this.indexOf(str);
  }
}
if(!String.prototype.trim){
  String.prototype.trim = function (str) {
    return this.replace(/^\s*/, "").replace(/\s*$/, "");
  }
}

var JBD = new function() {
  this.stripPortletOutputDecoration = function(html) {
    var lines = html.split("\n");
    var index = 0;
    while (true) {
      var line = lines[index];
      if (line.trim() == ""
          || line.startsWith("<link type=\"text/css")
          || line.startsWith("<!--[if IE]>")
          || line.startsWith("<![endif]-->")
          || line.startsWith("<script type=\"text/javascript")
          ) {
        // decoration from RunPortlet...
      } else {
        break;
      }
      index++;
    }
    html = lines.splice(index).join("\n");
    html = "<div class='wrappedPortlet'>"+html+"</div>";
    return html;
  };
};
