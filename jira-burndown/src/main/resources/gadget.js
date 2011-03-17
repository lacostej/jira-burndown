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
    if (prefs) {
      this.prefs = prefs
    } else {
      if (!this.prefs) {
        this.prefs = new gadgets.Prefs();
      }
    }
    if (! this.prefs) {
      throw "gadgets.Prefs() never initialized";
    }
    return {
      title : this.prefs.getString("Title"),
      versionId : this.prefs.getInt("VersionId"),
      typeId : this.prefs.getInt("TypeId"),
      height : this.prefs.getInt("Height"),
      width : this.prefs.getInt("Width"),
      includeLegend : this.prefs.getBool("IncludeLegend"),
      includeTrendline : this.prefs.getBool("IncludeTrendline"),
      startDate : this.prefs.getString("StartDate")
    };
  };
  
  this.runPortletPath = function() {
    var conf = this.config();
    return "/secure/RunPortlet.jspa?portletKey=com.laughingpanda.jira:versionWorkloadChart&chart.height="
                + conf.height + "&chart.width=" + conf.width + "&startDate=" + conf.startDate
                + "&versionId=" + conf.versionId + "&typeId=" + conf.typeId
                + "&chart.includeLegend=" + conf.includeLegend + "&chart.includeTrendline=" + conf.includeTrendline;
  };
};
