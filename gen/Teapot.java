import java.util.*;
import java.util.regex.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import java.net.URL;
import java.net.URLEncoder;

import java.math.BigDecimal;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.xml.sax.InputSource;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.*;
import org.javatuples.Pair;

import net.arnx.jsonic.JSON;

public class Teapot {

  public static final String version = "0.11.10b";
  public static final String RED = "\u001b[31m";
  public static final String RESET = "\u001b[m";

  public static Map<String,String> optionalProperties;
  public static List<String> opts;
  public static final String SP = "--";
  public static String indent = "";
  public static String toDirectory = "";

  public static int rowCount = 100;
  public static int errorCount = 0;
  public static List<String> downloadList = new ArrayList<String>();

  public static String download(String _url){
    URL url = null;
    InputStream is = null;
    String ret = null;
    try{
      url = new URL(_url);
      is = url.openStream();
      ret = IOUtils.toString(is);
    }catch(Exception e){
      return null;
    }finally{
      try{ if(is != null) is.close(); }catch(IOException e){ e.printStackTrace(); };
    }

    return ret;
  }

  public static byte[] downloadAsBytes(String _url){
    URL url = null;
    InputStream is = null;
    byte[] ret = null;
    try{
      url = new URL(_url);
      is = url.openStream();
      ret = IOUtils.toByteArray(is);
    }catch(Exception e){
      return null;
    }finally{
      try{ if(is != null) is.close(); }catch(IOException e){ e.printStackTrace(); };
    }

    return ret;
  }

  public static void println(Object o){
    System.out.println(o);
  }

  public static void help(){
    println("");
    println(String.format("Teapot version %s",version));
    println("");
    println("--------------------------------------------------");
    println("");
    println("usage: teapot [command] [args...] [option...]");
    println("");
    println("command:");
    println("");
    println("    help");
    println("                --- show help");
    println("");
    println("    search [query]");
    println("                --- search jar with query");
    println("");
    println("    versions [group-id] [artifact]");
    println("                --- search versions of [group-id].[artifact]");
    println("");
    println("    install [group-id] [artifact] [version]");
    println("                --- install jars and dependencies (without test scope)");
    println("");
    println("    install-all [group-id] [artifact] [version]");
    println("                --- install jars and dependencies (include test scope and optional)");
    println("");
    println("    jar [group-id] [artifact] [version]");
    println("                --- install jar only");
    println("");
    println("    pom [group-id] [artifact] [version]");
    println("                --- download pom-file [version] of [group-id].[artifact]");
    println("");
    println("option:");
    println("");
    println("    -v | --version");
    println("                --- print version");
    println("");
    println("    -h | --help");
    println("                --- show help");
    println("");
    println("    -Dproperty=value");
    println("                --- define property. use this if the property is undefined");
    println("");
    println("    -d [directory]");
    println("                --- set file output directory");
    println("");
    println("    -l [number]");
    println("                --- set list count for display");
    println("");
  }

  private static Document toXML(String xmlStr) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
      DocumentBuilder builder;  
      try{  
          builder = factory.newDocumentBuilder();  
          Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) ); 
          return doc;
      }catch (Exception e) {  
          e.printStackTrace();  
      } 
      return null;
  }

  public static void addDownloadList(String groupId, String artifactId, String version, String ext){
    String filename = String.format("%s:%s-%s.%s", groupId, artifactId, version, ext);
    if(!downloadList.contains(filename)){
      downloadList.add(filename);
    }
  }

  public static boolean isDownloaded(String groupId, String artifactId, String version, String ext){
    String filename = String.format("%s:%s-%s.%s", groupId, artifactId, version, ext);
    return downloadList.contains(filename);
  }

  public static String downloadFileAsString(String groupId, String artifact, String version, String ext){
    String id = groupId.replaceAll("\\.","/");
    String url = "http://search.maven.org/remotecontent?filepath="+id+"/"+artifact+"/"+version+"/"+artifact+"-"+version+"."+ext;
    String s = download(url);
    return s;
  }

  public static byte[] downloadFileAsBytes(String groupId, String artifact, String version, String ext){
    String id = groupId.replaceAll("\\.","/");
    String url = "http://search.maven.org/remotecontent?filepath="+id+"/"+artifact+"/"+version+"/"+artifact+"-"+version+"."+ext;
    byte[] s = downloadAsBytes(url);
    return s;
  }

  public static Document pom(String groupId, String artifact, String version){
    String s = downloadFileAsString(groupId, artifact, version, "pom");
    if(s != null){
      return toXML(s);
    }else{
      return null;
    }
  }

  public static NodeList xpath(String xpathStr, Document node){
    try{
      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();
      XPathExpression xpathExpr = xpath.compile(xpathStr);
      return (NodeList)xpathExpr.evaluate(node, XPathConstants.NODESET);
    }catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  public static void writeFile(byte[] bytes, String directory, String artifactId, String version, String ext){
    try{
      String path = directory.equals("") ? artifactId+"-"+version+"."+ext : directory+"/"+artifactId+"-"+version+"."+ext;
      FileOutputStream fos = new FileOutputStream(path);
      fos.write(bytes);
      fos.close();
    }catch(IOException e){
      e.printStackTrace();
      System.exit(1);
    }
  }

  /*
    @return status, versions
  */
  public static Pair<String,List<String>> parseVersionRange(String groupId, String artifactId, String version){
    String leftFlag = null;
    String rightFlag = null;
    String leftValue = null;
    String rightValue = null;

    StringBuffer sb = new StringBuffer();

    boolean start = true;
    boolean left = false;
    boolean right = false;

    String line = version;

    // Parse Range ==================================

    for(int i=0; i<line.length(); i++) {
      String s = line.substring(i,i+1);

      if(start){
        if(s.equals("(")){
          leftFlag = "gt";
        }else if(s.equals("[")){
          leftFlag = "gte";
        }else{
          println(String.format(RED+"[Parse Error] (column:%d) '%s' is not expected"+RESET, i, s));
          return null;
        }
        start = false;
        left = true;
      }else if(left){
        if(s.equals(",")){
          leftValue = sb.toString();
          sb = new StringBuffer();
          left = false;
          right = true;
        }else if(s.equals("(") || s.equals("[") || s.equals(")") || s.equals("]")){
          println(String.format(RED+"[Parse Error] (column:%d) '%s' is not expected"+RESET, i, s));
          return null;
        }else{
          sb.append(s);
        }
      }else if(right){
        if(s.equals(")")){
          rightValue = sb.toString();
          rightFlag = "lt";
          break;
        }else if(s.equals("]")){
          rightValue = sb.toString();
          rightFlag = "lte";
          break;
        }else if(s.equals("(") || s.equals("[") || s.equals(",")){
          println(String.format(RED+"[Parse Error] (column:%d) '%s' is not expected"+RESET, i, s));
          return null;
        }else{
          sb.append(s);
        }
      }
    }

    // Get Versions =============================

    List<String> versions = versionsAsList(groupId, artifactId);
    Collections.reverse(versions); // reverse!

    if(!leftValue.equals("")){
      if(leftFlag.equals("gte")){
        int n = versions.indexOf(leftValue);
        versions.subList(0,n).clear();
      }else if(leftFlag.equals("gt")){
        int n = versions.indexOf(leftValue);
        versions.subList(0,n+1).clear();
      }
    }

    Collections.reverse(versions);

    if(!rightValue.equals("")){
      if(rightFlag.equals("lte")){
        int n = versions.indexOf(rightValue);
        versions.subList(0,n).clear();
      }else if(rightFlag.equals("lt")){
        int n = versions.indexOf(rightValue);
        versions.subList(0,n+1).clear();
      }
    }

    String status = null;
    if(leftValue.equals("")){
      status = "lt";
    }else if(rightValue.equals("")){
      status = "gt";
    }else{
      status = "range";
    }

    Pair<String,List<String>> tuple = Pair.with(status, versions); 
    return tuple;
  }

  /** Parse version like "(1.2, 3.4]" */
  public static String detectVersion(String groupId, String artifactId, String version){
    Pair<String,List<String>> tuple = parseVersionRange(groupId, artifactId, version);

    if(tuple == null) return null;

    String status = tuple.getValue0();
    List<String> versions = tuple.getValue1();

    String ret = null;
    if(status.equals("lt")){
      ret = versions.get(0);
    }else if(status.equals("gt")){
      ret = versions.get(versions.size()-1);
    }else{
      ret = versions.get(0);
    }

    return ret;
  }

  // DownloadPom =============================================================

  public static void downloadPom(String groupId, String artifact, String version){
    downloadPom(groupId, artifact, version, toDirectory);
  }
  public static void downloadPom(String groupId, String artifact, String version, String directory){
    println("[Phase 1/1] Downloading Pom...");
    byte[] jarBytes = downloadFileAsBytes(groupId, artifact, version, "pom");
    writeFile(jarBytes, directory, artifact, version, "pom");
    println("\nComplete!");
  }

  // DownloadJar =============================================================

  public static void downloadJar(String groupId, String artifact, String version){
    downloadJar(groupId, artifact, version, toDirectory);
  }
  public static void downloadJar(String groupId, String artifact, String version, String directory){
    println("[Phase 1/1] Downloading Jar...");
    byte[] jarBytes = downloadFileAsBytes(groupId, artifact, version, "jar");
    writeFile(jarBytes, directory, artifact, version, "jar");
    println("\nComplete!");
  }

  // Install =============================================================

  public static void installAll(String groupId, String artifact, String version){
    install(groupId, artifact, version, toDirectory, true, true);
  }
  public static void installAll(String groupId, String artifact, String version, String directory){
    install(groupId, artifact, version, toDirectory, true, true);
  }

  public static void install(String groupId, String artifact, String version){
    install(groupId, artifact, version, toDirectory, true, false);
  }
  public static void install(String groupId, String artifact, String version, String directory){
    install(groupId, artifact, version, toDirectory, true, false);
  }

  public static void install(String groupId, String artifact, String version, String directory, Boolean root, Boolean installAll){
    
    // =====================
    // Install Start
    // =====================

    if(root){
      println(String.format("\nInstalling %s:%s (%s)...\n", groupId, artifact, version));
    }else{
      println(String.format(indent + " " + "Installing %s:%s (%s)...", groupId, artifact, version));
    }

    // =====================
    // Download Pom
    // =====================

    if(root) println("[Phase 1/4] Downloading Pom...");

    Document xml = pom(groupId, artifact, version);

    if(xml == null){
      println(String.format(RED + "[Error] %s:%s (%s) was not found" + RESET, groupId, artifact, version));
      errorCount += 1;
      return;
    }

    // =====================
    // Read Properties
    // =====================

    if(root) println("[Phase 2/4] Reading Properties...");

    Map<String,String> properties = new HashMap<String,String>();
    NodeList propertiesXML = xpath("//project/properties/*", xml);
    for(int i=0; i<propertiesXML.getLength(); i++){
      Element property = (Element)propertiesXML.item(i);
      String name = property.getTagName();
      String value = property.getTextContent();

      properties.put(name,value);
      // if(root) println(String.format("    %s = %s", name, value));
    }

    // =====================
    // Install Dependencies
    // =====================

    if(root) println("[Phase 3/4] Installing Dependencies...");

    NodeList dependencies = xpath("//project/dependencies/dependency", xml);

    // Read Parent Properties

    try{
      properties.put("parent.version", ((NodeList)xpath("//project/parent/version", xml)).item(0).getTextContent());
      properties.put("parent.groupId", ((NodeList)xpath("//project/parent/groupId", xml)).item(0).getTextContent());
      properties.put("parent.artifactId", ((NodeList)xpath("//project/parent/artifactId", xml)).item(0).getTextContent());
    }catch(Exception e){}

    // Read Pom Properties

    try{
      properties.put("pom.version", ((NodeList)xpath("//project/version", xml)).item(0).getTextContent());
      properties.put("pom.groupId", ((NodeList)xpath("//project/groupId", xml)).item(0).getTextContent());
      properties.put("pom.artifactId", ((NodeList)xpath("//project/artifactId", xml)).item(0).getTextContent());

      properties.put("project.version", properties.get("pom.version"));
      properties.put("project.groupId", properties.get("pom.groupId"));
      properties.put("project.artifactId", properties.get("pom.artifactId"));
    }catch(Exception e){}

    // ================================================
    // Install Dependencies
    // ================================================

    for(int i=0; i<dependencies.getLength(); i++){
      Element dependency = (Element)dependencies.item(i);

      NodeList _groupIdElement = dependency.getElementsByTagName("groupId");
      NodeList _artifactIdElement = dependency.getElementsByTagName("artifactId");
      NodeList _versionElement = dependency.getElementsByTagName("version");

      // Skip test and optional

      NodeList optionalElement = dependency.getElementsByTagName("optional");
      NodeList scopeElement = dependency.getElementsByTagName("scope");

      if(!installAll){
        if(scopeElement.getLength() > 0){
          String scope = scopeElement.item(0).getTextContent();
          if(scope.equals("test") || scope.equals("provided")){
            continue; // skip
          }
        }

        if(optionalElement.getLength() > 0){
          String optional = optionalElement.item(0).getTextContent();
          if(optional.equals("true")){
            continue; // optional then skip
          }
        }
      }

      String _groupId = _groupIdElement.item(0).getTextContent();
      String _artifactId =  _artifactIdElement.item(0).getTextContent();
      String _version = null;

      // Set Version

      if(_versionElement.getLength() > 0){
        _version = _versionElement.item(0).getTextContent();
      }else{
        // Set Latest Version
        List<String> allVersions = versionsAsList(_groupId, _artifactId);
        _version = allVersions.get(0);
      }

      // WORKAROUND

      if(_groupId.equals("xerces") && _artifactId.equals("xerces-impl")){
        _artifactId = "xercesImpl";
      }

      // Set Properties
      if(_groupId.indexOf("$") > -1){
        String s = _groupId;
        s = s.replaceAll("\\$","");
        s = s.replaceAll("\\{","");
        s = s.replaceAll("\\}","");
        String v = properties.get(s);
        if(v != null){
          _groupId = v;
        }else if(optionalProperties.get(s) != null){
          _groupId = optionalProperties.get(s);
        }else{
          println(RED + "[Error] parameter '"+s+"' is undefined" + RESET);
          continue;
        }
      }
      if(_artifactId.indexOf("$") > -1){
        String s = _artifactId;
        s = s.replaceAll("\\$","");
        s = s.replaceAll("\\{","");
        s = s.replaceAll("\\}","");
        String v = properties.get(s);
        if(v != null){
          _artifactId = v;
        }else if(optionalProperties.get(s) != null){
          _artifactId = optionalProperties.get(s);
        }else{
          println(RED + "[Error] parameter '"+s+"' is undefined" + RESET);
          continue;
        }
      }
      if(_version.indexOf("$") > -1){
        String s = _version;
        s = s.replaceAll("\\$","");
        s = s.replaceAll("\\{","");
        s = s.replaceAll("\\}","");
        String v = properties.get(s);
        if(v != null){
          _version = v;
        }else if(s.equals("project.version")){
          _version = properties.get("parent.version");
        }else if(optionalProperties.get(s) != null){
          _version = optionalProperties.get(s);
        }else{
          println(RED + "[Error] parameter '"+s+"' is undefined" + RESET);
          continue;
        }
      }

      String versionRange = null;

      // Detect version of Range
      if(_version.startsWith("[") || _version.startsWith("(")){
        versionRange = _version;
        _version = detectVersion(_groupId, _artifactId, _version);
        if(_version == null){
          continue;
        }
      }

      // Treat wild card
      if(_version.indexOf("*") > -1){
        List<String> allVersions = versionsAsList(_groupId, _artifactId);
        _version = allVersions.get(0);
      }

      // Install Dependency Recursively
      if(!isDownloaded(_groupId, _artifactId, _version, "jar")){
        String oldIndent = indent;
        indent += SP;
        addDownloadList(_groupId, _artifactId, _version, "jar");
        install(_groupId, _artifactId, _version, directory, false, installAll);
        indent = oldIndent;
      }
    }

    // ============================================
    // End of Install Dependencies
    // =============================================

    if(root) println("");

    // =====================
    // Install Jar
    // =====================

    if(root) println(String.format("[Phase 4/4] Installing %s:%s (%s)...", groupId, artifact, version));
    
    byte[] jarBytes = downloadFileAsBytes(groupId, artifact, version, "jar");
    writeFile(jarBytes, directory, artifact, version, "jar");

    if(root){
      if(errorCount == 0){
        println("\nComplete!");
      }else{
        println("\n"+RED+"Install failed ("+errorCount+" errors occured)"+RESET);
      }
    }
  }

  // Versions =============================================================

  public static void versions(String groupId, String artifact){
    String s = download("http://search.maven.org/solrsearch/select?q=g:%22"+groupId+"%22+AND+a:%22"+artifact+"%22&core=gav&rows="+rowCount+"&wt=json");
    Map<String,Object> result = JSON.decode(s);
    Map<String,Object> response = (Map<String,Object>)result.get("response");
    ArrayList<Map<String,Object>> docs = (ArrayList<Map<String,Object>>)response.get("docs");
    
    ArrayList<String> versions = new ArrayList<String>();
    for (Map<String,Object> doc : docs) {
      versions.add((String)doc.get("v"));
    }

    println("\nVersions:\n");

    for (String version : versions) {
      println(String.format("%s", version));
    }

    BigDecimal numFound = (BigDecimal)response.get("numFound");
    BigDecimal count = numFound.subtract(new BigDecimal(rowCount));
    if(count.compareTo(BigDecimal.ZERO) == 1){
      println("\n and more ... (rest: "+count+")");
    }
  }

  // VersionsAsList =============================================================

  public static List<String> versionsAsList(String groupId, String artifact){
    String s = download("http://search.maven.org/solrsearch/select?q=g:%22"+groupId+"%22+AND+a:%22"+artifact+"%22&core=gav&rows="+rowCount+"&wt=json");
    Map<String,Object> result = JSON.decode(s);
    Map<String,Object> response = (Map<String,Object>)result.get("response");
    ArrayList<Map<String,Object>> docs = (ArrayList<Map<String,Object>>)response.get("docs");
    
    ArrayList<String> versions = new ArrayList<String>();
    for (Map<String,Object> doc : docs) {
      versions.add((String)doc.get("v"));
    }

    return versions;
  }

  // Search =============================================================

  public static void search(String query){
    String enQuery = URLEncoder.encode(query);
    String s = download("http://search.maven.org/solrsearch/select?q="+enQuery+"&rows="+rowCount+"&wt=json");
    Map<String,Object> result = JSON.decode(s);
    Map<String,Object> response = (Map<String,Object>)result.get("response");
    ArrayList<Map<String,Object>> docs = (ArrayList<Map<String,Object>>)response.get("docs");
    
    ArrayList<String> groups = new ArrayList<String>();
    ArrayList<String> artifacts = new ArrayList<String>();
    for (Map<String,Object> doc : docs) {
      groups.add((String)doc.get("g"));
      artifacts.add((String)doc.get("a"));
    }

    println("\nSearch Results:\n");

    for (int i=0; i<groups.size(); i++) {
      String group = groups.get(i);
      String artifact = artifacts.get(i);

      println(String.format("%s (%s)", artifact, group));
    }

    BigDecimal numFound = (BigDecimal)response.get("numFound");
    BigDecimal count = numFound.subtract(new BigDecimal(rowCount));
    if(count.compareTo(BigDecimal.ZERO) == 1){
      println("\n and more ... (rest: "+count+")");
    }
  }

  // Main =============================================================

  public static void main(String[] args) {
    opts = new ArrayList<String>();
    optionalProperties = new HashMap<String,String>();

    if(args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))){
      println(String.format("Teapot version %s",version));
      System.exit(0);
    }else if(args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))){
      help();
      System.exit(0);
    }

    int i = 0;
    while(i < args.length) {
      String arg = args[i];

      if(arg.startsWith("-D")){
        String s = arg.substring(2, arg.length());
        int pos = s.indexOf("=");
        String param = s.substring(0,pos);
        String value = s.substring(pos+1, s.length());
        optionalProperties.put(param,value);
      }else if(arg.equals("-d")){
        if(i == args.length - 1){
          help();
          System.exit(1);
        }else{
          toDirectory = args[i+1];
          i++;
        }
      }else if(arg.equals("-l")){
        if(i == args.length - 1){
          help();
          System.exit(1);
        }else{
          rowCount = Integer.parseInt(args[i+1]);
          i++;
        }
      }else{
        opts.add(arg);
      }

      i++;
    }

    if(opts.size() == 0 || opts.get(0).equals("help")){
      help();

    }else if(opts.size() == 2 && opts.get(0).equals("search")){
      search(opts.get(1));

    }else if(opts.size() == 3 && opts.get(0).equals("versions")){
      versions(opts.get(1), opts.get(2));

    }else if(opts.size() == 4 && opts.get(0).equals("install")){
      install(opts.get(1), opts.get(2), opts.get(3));

    }else if(opts.size() == 4 && opts.get(0).equals("install-all")){
      installAll(opts.get(1), opts.get(2), opts.get(3));

    }else if(opts.size() == 4 && opts.get(0).equals("pom")){
      downloadPom(opts.get(1), opts.get(2), opts.get(3));

    }else if(opts.size() == 4 && opts.get(0).equals("jar")){
      downloadJar(opts.get(1), opts.get(2), opts.get(3));

    }else{
      help();
    }
  }
}