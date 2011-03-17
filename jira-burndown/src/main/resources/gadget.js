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
  
  this.config = function(prefs) {
    return {
      title : prefs.getString("Title"),
      versionId : prefs.getInt("VersionId"),
      typeId : prefs.getInt("TypeId"),
      height : prefs.getInt("Height"),
      width : prefs.getInt("Width"),
      includeLegend : prefs.getBool("IncludeLegend"),
      includeTrendline : prefs.getBool("IncludeTrendline"),
      startDate : prefs.getString("StartDate")
    };
  };
  
  this.runPortletPath = function(conf) {
    return "/secure/RunPortlet.jspa?portletKey=com.laughingpanda.jira:versionWorkloadChart&chart.height="
                + conf.height + "&chart.width=" + conf.width + "&startDate=" + conf.startDate
                + "&versionId=" + conf.versionId + "&typeId=" + conf.typeId
                + "&chart.includeLegend=" + conf.includeLegend + "&chart.includeTrendline=" + conf.includeTrendline;
  };
};
